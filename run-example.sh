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