#!/bin/bash

# Test script to verify PathConstraintAPI access
set -e

echo "Testing PathConstraintAPI access..."

# Build if needed
mvn compile -q

# Generate classpath
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q
CP="target/classes:$(cat cp.txt)"

# Test just the API access without full agent
echo "1" | java -cp "$CP" edu.neu.ccs.prl.galette.examples.ModelTransformationExample

echo "✅ Basic application works without agent"