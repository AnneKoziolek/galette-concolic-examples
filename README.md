# Galette Concolic Execution Examples

This project demonstrates the Galette concolic execution framework with automatic comparison interception and Knarr symbolic execution integration.

## Architecture

This is a **separate project** that depends on the Galette core implementation. This separation is crucial for proper agent instrumentation:

- **galette-concolic-model-transformation/**: Core Galette implementation (agent, runtime)
- **galette-concolic-examples/**: Example applications (this project)

## Key Features Demonstrated

1. **Automatic Comparison Interception**: Galette agent automatically instruments comparison operations (`>`, `<`, `==`, etc.) without code changes
2. **Concolic Execution**: Combines concrete execution with symbolic execution for systematic path exploration
3. **Model Transformation Analysis**: Shows how external inputs affect model transformation results
4. **Path Constraint Collection**: Automatically collects constraints for test generation and analysis

## Running the Examples

### Prerequisites

- JDK 17+
- Maven 3.6+
- Built Galette dependencies (the script will build them automatically)

### Quick Start

```bash
# Run the main demonstration
./run-example.sh

# Then select option 2 for concolic execution
```

### What the Example Does

The example demonstrates a brake disc model transformation where:

1. **Input**: Brake disc thickness (external user input)
2. **Logic**: `if (thickness > STIFFNESS_THRESHOLD) additionalStiffness = true`
3. **Analysis**: Galette automatically:
   - Intercepts the comparison `thickness > STIFFNESS_THRESHOLD`
   - Collects path constraints like `thickness > 60.0`
   - Generates alternative inputs to explore different paths
   - Shows how input values affect transformation outcomes

### Expected Output

With proper instrumentation, you should see:

```
🔧 GaletteTransformer: interceptorEnabled = true for class BrakeDiscTransformation
🔧 Adding ComparisonInterceptorVisitor for class: BrakeDiscTransformation
🔍 PathUtils.instrumentedDcmpl called: 12.0 vs 60.0
✅ DCMPG constraint added: 12.0 DCMPG 60.0 -> -1
🔧 Retrieved 1 raw constraints from Galette PathUtils
Path constraints: thickness < 60.0
```

## Project Structure

```
galette-concolic-examples/
├── src/main/java/edu/neu/ccs/prl/galette/examples/
│   ├── ModelTransformationExample.java          # Main demo application
│   ├── models/
│   │   ├── source/BrakeDiscSource.java         # Source model
│   │   └── target/BrakeDiscTarget.java         # Target model  
│   └── transformation/
│       └── BrakeDiscTransformation.java        # Business logic with comparisons
├── run-example.sh                              # Execution script with agent setup
├── pom.xml                                     # Maven dependencies
└── README.md                                   # This file
```

## Technical Details

### Why a Separate Project?

Java agents typically only instrument **external classes**. If the example code were in the same project as the agent, it wouldn't be properly instrumented. This separation ensures:

1. ✅ **Proper Instrumentation**: Example classes are treated as external and get instrumented
2. ✅ **Realistic Testing**: Simulates how real users would integrate Galette
3. ✅ **Clean Dependencies**: Clear separation between core framework and examples

### Agent Configuration

The `run-example.sh` script configures the Galette agent with:

- `-javaagent:galette-agent.jar`: Enables bytecode instrumentation
- `-Xbootclasspath/a:galette-agent.jar`: Makes agent classes available  
- `-Dgalette.concolic.interception.enabled=true`: Enables automatic comparison interception
- Instrumented Java runtime for optimal performance

### Integration with Knarr

The example shows how to integrate Galette's automatic interception with Knarr's symbolic execution:

1. **Automatic Collection**: Galette automatically collects comparison constraints
2. **Bridge Integration**: `GalettePathConstraintBridge` converts Galette constraints to Green expressions
3. **Path Exploration**: Knarr uses constraints to generate inputs for unexplored paths

## Troubleshooting

### No Constraints Collected

If you see "Path constraints: no constraints", check:

1. **Agent Loading**: Look for `GaletteTransformer` debug output
2. **Instrumentation**: Verify classes are being processed by the agent
3. **System Properties**: Ensure `galette.concolic.interception.enabled=true`

### Build Issues

If dependencies are missing:

```bash
# Build and install Galette dependencies
cd ../galette-concolic-model-transformation/galette-agent
mvn clean install -DskipTests

cd ../knarr-runtime  
mvn clean install -DskipTests

# Then build examples
cd ../../galette-concolic-examples
mvn clean compile
```

## Next Steps

This example provides a foundation for:

1. **Custom Model Transformations**: Adapt the brake disc example for your domain
2. **Integration Testing**: Use the pattern to test Galette with your applications  
3. **Performance Analysis**: Measure the impact of automatic instrumentation
4. **Tool Development**: Build tools that leverage automatic path constraint collection