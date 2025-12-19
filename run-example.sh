#!/bin/bash

# Script to run the ModelTransformationExample with Galette instrumentation
# This script demonstrates proper usage of Galette agent from an external project

set -e  # Exit on any error

# Ensure Java 17 is used for builds and execution
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

echo "🚀 Galette Concolic Examples"
echo "============================="
echo "☕ Java Configuration:"
echo "   JAVA_HOME: $JAVA_HOME"
echo "   Java version: $(java -version 2>&1 | head -1)"
echo ""

# Configuration
GALETTE_PROJECT_DIR="../galette-concolic-model-transformation"
GALETTE_AGENT_JAR="$GALETTE_PROJECT_DIR/galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"
GREEN_SOLVER_DIR="../green-solver"
GREEN_SERVER_PORT=9408
GREEN_SERVER_PID=""

# Function to check if GreenServer is already running
is_green_server_running() {
    nc -z localhost $GREEN_SERVER_PORT 2>/dev/null
    return $?
}

# Function to start the GreenServer in a separate non-instrumented JVM
start_green_server() {
    echo "🔧 Starting GreenServer (non-instrumented JVM for solver isolation)..."
    
    if is_green_server_running; then
        echo "✅ GreenServer already running on port $GREEN_SERVER_PORT"
        return 0
    fi
    
    # Build greenserver if needed
    local GREENSERVER_DIR="$GREEN_SOLVER_DIR/greenserver"
    local GREEN_JAR="$GREEN_SOLVER_DIR/green/target/green-1.0-SNAPSHOT.jar"
    
    # Build green if JAR doesn't exist
    if [ ! -f "$GREEN_JAR" ]; then
        echo "🔨 Building green solver..."
        (cd "$GREEN_SOLVER_DIR/green" && mvn package -DskipTests -q)
    fi
    
    # Copy green JAR to greenserver lib if needed
    if [ ! -f "$GREENSERVER_DIR/lib/green.jar" ] || [ "$GREEN_JAR" -nt "$GREENSERVER_DIR/lib/green.jar" ]; then
        echo "📦 Updating greenserver/lib/green.jar..."
        cp "$GREEN_JAR" "$GREENSERVER_DIR/lib/green.jar"
    fi
    
    # Build greenserver using javac (ant may not be installed)
    echo "🔨 Building greenserver..."
    local GS_SRC="$GREENSERVER_DIR/src/za/ac/sun/cs/green/server/GreenServer.java"
    local GS_CLASS="$GREENSERVER_DIR/bin/za/ac/sun/cs/green/server/GreenServer.class"
    if [ ! -f "$GS_CLASS" ] || [ "$GS_SRC" -nt "$GS_CLASS" ]; then
        mkdir -p "$GREENSERVER_DIR/bin/za/ac/sun/cs/green/server"
        javac -cp "$GREENSERVER_DIR/lib/green.jar" -d "$GREENSERVER_DIR/bin" "$GS_SRC" 2>&1
        if [ $? -ne 0 ]; then
            echo "❌ Failed to compile GreenServer"
            return 1
        fi
    fi
    
    # Build classpath for greenserver
    local GREEN_LIB="$GREEN_SOLVER_DIR/green/lib"
    local KNARR_Z3_LIB="../knarr/z3-4.8.9-x64-ubuntu-16.04/bin"
    local SERVER_CP="$GREENSERVER_DIR/bin:$GREENSERVER_DIR/lib/green.jar"
    # Use Z3 JAR from knarr for Linux compatibility
    SERVER_CP="$SERVER_CP:$KNARR_Z3_LIB/com.microsoft.z3.jar"
    SERVER_CP="$SERVER_CP:$GREEN_LIB/slf4j-api-1.7.12.jar"
    SERVER_CP="$SERVER_CP:$GREEN_LIB/slf4j-simple-1.7.12.jar"
    
    # Set Z3 native library path (Linux .so files are in knarr)
    export LD_LIBRARY_PATH="$KNARR_Z3_LIB:$GREEN_LIB:$LD_LIBRARY_PATH"
    
    # Start the server in background using NON-instrumented Java
    echo "🚀 Starting GreenServer on port $GREEN_SERVER_PORT..."
    java -cp "$SERVER_CP" za.ac.sun.cs.green.server.GreenServer > /tmp/greenserver.log 2>&1 &
    GREEN_SERVER_PID=$!
    
    # Wait for server to start
    echo "⏳ Waiting for GreenServer to start..."
    local MAX_WAIT=30
    local WAITED=0
    while ! is_green_server_running && [ $WAITED -lt $MAX_WAIT ]; do
        sleep 0.5
        WAITED=$((WAITED + 1))
        if ! kill -0 $GREEN_SERVER_PID 2>/dev/null; then
            echo "❌ GreenServer process died. Check /tmp/greenserver.log"
            cat /tmp/greenserver.log
            return 1
        fi
    done
    
    if is_green_server_running; then
        echo "✅ GreenServer started (PID: $GREEN_SERVER_PID)"
        return 0
    else
        echo "❌ GreenServer failed to start within ${MAX_WAIT}s"
        cat /tmp/greenserver.log
        return 1
    fi
}

# Function to stop the GreenServer
stop_green_server() {
    if [ -n "$GREEN_SERVER_PID" ] && kill -0 $GREEN_SERVER_PID 2>/dev/null; then
        echo "🛑 Stopping GreenServer (PID: $GREEN_SERVER_PID)..."
        kill $GREEN_SERVER_PID 2>/dev/null || true
        wait $GREEN_SERVER_PID 2>/dev/null || true
        echo "✅ GreenServer stopped"
    fi
}

# Cleanup on exit
cleanup() {
    stop_green_server
}
trap cleanup EXIT

# Function to check if dependencies are built
check_dependencies() {
    echo "🔍 Checking Galette dependencies..."
    
    if [ ! -f "$GALETTE_AGENT_JAR" ]; then
        echo "❌ Galette agent JAR not found at: $GALETTE_AGENT_JAR"
        echo "   Building Galette dependencies..."
        (cd "$GALETTE_PROJECT_DIR/galette-agent" && \
         MAVEN_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED" \
         mvn clean package -DskipTests -q)
        echo "✅ Galette agent built successfully"
    else
        echo "✅ Galette agent JAR found"
    fi
    
    # Check if knarr-runtime is installed in local repo
    if [ ! -d "$HOME/.m2/repository/edu/neu/ccs/prl/galette/knarr-runtime" ]; then
        echo "📦 Installing knarr-runtime to local repository..."
        (cd "$GALETTE_PROJECT_DIR/knarr-runtime" && mvn install -DskipTests -q)
        echo "✅ knarr-runtime installed"
    else
        echo "✅ knarr-runtime found in local repository" 
    fi
    
    # Install galette-agent to local repo if needed
    if [ ! -d "$HOME/.m2/repository/edu/neu/ccs/prl/galette/galette-agent" ]; then
        echo "📦 Installing galette-agent to local repository..."
        (cd "$GALETTE_PROJECT_DIR/galette-agent" && \
         MAVEN_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED" \
         mvn install -DskipTests -q)
        echo "✅ galette-agent installed"
    else
        echo "✅ galette-agent found in local repository"
    fi
}

# Function to build examples project
build_examples() {
    echo "🔨 Building examples project..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
    echo "✅ Examples project built successfully"
}

# Function to create instrumented Java if needed
setup_instrumented_java() {
    local instrumented_java_dir="target/galette/java"
    
    if [ ! -d "$instrumented_java_dir" ]; then
        echo "⚙️ Creating instrumented Java installation..." >&2
        mkdir -p target/galette
        
        # Use the galette-instrument from the dependency project
        local instrument_jar="$GALETTE_PROJECT_DIR/galette-instrument/target/galette-instrument-1.0.0-SNAPSHOT.jar"
        if [ ! -f "$instrument_jar" ]; then
            echo "🔨 Building galette-instrument..." >&2
            (cd "$GALETTE_PROJECT_DIR/galette-instrument" && mvn package -DskipTests -q)
        fi
        
        
        # Create instrumented Java with proper error handling
        echo "🔧 Instrumenting Java from $JAVA_HOME to $instrumented_java_dir" >&2
        if java -jar "$instrument_jar" "$JAVA_HOME" "$instrumented_java_dir" > /tmp/instrument.log 2>&1; then
            echo "✅ Instrumented Java created" >&2
        else
            echo "❌ Failed to create instrumented Java. See /tmp/instrument.log for details." >&2
            echo "   Will use regular Java instead." >&2
            return
        fi
    else
        echo "⚡ Using existing instrumented Java" >&2
    fi
    
    # Return the absolute path (only this goes to stdout)
    echo "$(pwd)/$instrumented_java_dir"
}

# Main execution
echo

# Start GreenServer FIRST (before other builds, so it has time to start)
start_green_server
echo

check_dependencies
echo
build_examples
echo

# Setup instrumented Java
INSTRUMENTED_JAVA=$(setup_instrumented_java)

if [ ! -f "$INSTRUMENTED_JAVA/bin/java" ]; then
    echo "❌ Instrumented Java not found at: $INSTRUMENTED_JAVA"
    echo "   Cannot run Galette properly without instrumented Java!"
    exit 1
else
    JAVA_CMD="$INSTRUMENTED_JAVA/bin/java"
    echo "✅ Using instrumented Java: $JAVA_CMD"
fi

# Generate classpath
echo "📋 Generating classpath..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q

if [ ! -f cp.txt ]; then
    echo "❌ Failed to generate classpath file!"
    exit 1
fi

# Create classpath
CP="target/classes:$(cat cp.txt)"

echo "📚 Using classpath with $(echo $CP | tr ':' '\n' | wc -l) entries"
echo

# Run with Galette agent
echo "🚀 Running ModelTransformationExample with Galette instrumentation..."
echo "   Command: $JAVA_CMD"
echo "   Agent: $GALETTE_AGENT_JAR"
echo "   Instrumentation: ENABLED"
echo

# Create cache directory
mkdir -p target/galette/cache

# Run the example
"$JAVA_CMD" \
  -cp "$CP" \
  -Xbootclasspath/a:"$GALETTE_AGENT_JAR" \
  -javaagent:"$GALETTE_AGENT_JAR" \
  -Dgalette.cache=target/galette/cache \
  -Dgalette.concolic.interception.enabled=true \
  -Dgalette.concolic.interception.debug=true \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample "$@"

echo ""
echo "✅ Execution completed"
echo ""
echo "📋 Summary:"
echo "   ✅ External project structure ensures proper agent instrumentation"
echo "   ✅ Classes are loaded and instrumented by Galette agent at runtime"
echo "   ✅ Automatic comparison interception should now work correctly"