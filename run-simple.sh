#!/bin/bash

# Simple script to test Galette agent without complex setup
set -e

echo "🚀 Simple Galette Test"
echo "====================="

# Determine JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    echo "🔍 JAVA_HOME not set, attempting to detect..."
    # Try to find JAVA_HOME from java command
    if command -v java >/dev/null 2>&1; then
        JAVA_EXECUTABLE=$(command -v java)
        # Follow symlinks to get real path
        JAVA_EXECUTABLE=$(readlink -f "$JAVA_EXECUTABLE" 2>/dev/null || realpath "$JAVA_EXECUTABLE" 2>/dev/null || echo "$JAVA_EXECUTABLE")
        # Get JAVA_HOME by going up from bin/java
        JAVA_HOME=$(dirname "$(dirname "$JAVA_EXECUTABLE")")
        echo "✅ Detected JAVA_HOME: $JAVA_HOME"
        export JAVA_HOME
    else
        echo "❌ Could not detect Java installation. Please set JAVA_HOME."
        exit 1
    fi
else
    echo "✅ Using existing JAVA_HOME: $JAVA_HOME"
fi

# Build if needed
if [ ! -d "target/classes" ]; then
    echo "🔨 Building..."
    mvn compile -q
fi

# Generate classpath
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
CP="target/classes:$(cat cp.txt)"

# Get agent path
AGENT="../galette-concolic-model-transformation/galette-agent/target/galette-agent-1.0.0-SNAPSHOT.jar"

if [ ! -f "$AGENT" ]; then
    echo "❌ Agent not found: $AGENT"
    echo "🔨 Building galette-agent..."
    (cd ../galette-concolic-model-transformation/galette-agent && \
     MAVEN_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED" \
     mvn clean package -DskipTests -q)
    if [ ! -f "$AGENT" ]; then
        echo "❌ Failed to build agent"
        exit 1
    fi
fi

echo "🔧 Running with Galette agent..."
echo "📋 Agent: $AGENT"

# Run with minimal agent configuration
echo "2" | java \
  -cp "$CP" \
  -javaagent:"$AGENT" \
  -Dgalette.concolic.interception.enabled=true \
  edu.neu.ccs.prl.galette.examples.ModelTransformationExample

echo ""
echo "✅ Test completed"