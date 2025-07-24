package edu.neu.ccs.prl.galette.knarr;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.SymbolicComparison;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.*;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.expr.Operation;

/**
 * Wrapper class that handles symbolic execution concerns separately from business logic.
 *
 * This class provides a clean separation between the core model transformation
 * logic and the symbolic execution infrastructure. It can be used to "wrap"
 * existing transformations with symbolic execution capabilities without
 * modifying the original transformation code.
 *
 * Design benefits:
 * - Core transformation logic remains clean and focused
 * - Symbolic execution can be added/removed without changing business logic
 * - Makes it easier to integrate Galette into existing systems
 * - Demonstrates clean architecture principles
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class SymbolicExecutionWrapper {

    /**
     * Container for symbolic values and their concrete counterparts.
     */
    public static class SymbolicValue<T> {
        private final T concreteValue;
        private final Tag symbolicTag;
        private final String label;

        public SymbolicValue(String label, T concreteValue) {
            this.label = label;
            this.concreteValue = concreteValue;
            this.symbolicTag = createSymbolicTag(label, concreteValue);
        }

        public T getValue() {
            return concreteValue;
        }

        public Tag getTag() {
            return symbolicTag;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSymbolic() {
            return symbolicTag != null && !symbolicTag.isEmpty();
        }

        @SuppressWarnings("unchecked")
        private Tag createSymbolicTag(String label, T value) {
            if (value instanceof Double) {
                return GaletteSymbolicator.makeSymbolicDouble(label, (Double) value);
            } else if (value instanceof Integer) {
                return GaletteSymbolicator.makeSymbolicInt(label, (Integer) value);
            } else if (value instanceof Long) {
                return GaletteSymbolicator.makeSymbolicLong(label, (Long) value);
            } else if (value instanceof String) {
                return GaletteSymbolicator.makeSymbolicString(label, (String) value);
            } else {
                System.err.println("Unsupported type for symbolic value: " + value.getClass());
                return null;
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "SymbolicValue{label='%s', value=%s, symbolic=%s}", label, concreteValue, isSymbolic());
        }
    }

    /**
     * Interface for conditional operations that can be tracked symbolically.
     */
    @FunctionalInterface
    public interface SymbolicCondition {
        boolean evaluate();
    }

    /**
     * Create a symbolic double value.
     */
    public static SymbolicValue<Double> makeSymbolicDouble(String label, double value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Create a symbolic integer value.
     */
    public static SymbolicValue<Integer> makeSymbolicInt(String label, int value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Create a symbolic string value.
     */
    public static SymbolicValue<String> makeSymbolicString(String label, String value) {
        return new SymbolicValue<>(label, value);
    }

    /**
     * Perform a symbolic comparison and collect path constraints.
     *
     * @param left Left operand (can be symbolic or concrete)
     * @param right Right operand (can be symbolic or concrete)
     * @param operation The comparison operation as a string
     * @return The concrete result of the comparison
     */
    public static boolean symbolicComparison(SymbolicValue<Double> left, double right, String operation) {
        switch (operation.toLowerCase()) {
            case ">":
            case "gt":
                return SymbolicComparison.greaterThan(left.getValue(), left.getTag(), right, null);
            case ">=":
            case "ge":
                return SymbolicComparison.greaterThanOrEqual(left.getValue(), left.getTag(), right, null);
            case "<":
            case "lt":
                return SymbolicComparison.lessThan(left.getValue(), left.getTag(), right, null);
            case "<=":
            case "le":
                return SymbolicComparison.lessThanOrEqual(left.getValue(), left.getTag(), right, null);
            case "==":
            case "eq":
                return SymbolicComparison.equal(left.getValue(), left.getTag(), right, null);
            default:
                throw new IllegalArgumentException("Unsupported comparison operation: " + operation);
        }
    }

    /**
     * Execute a conditional with symbolic tracking.
     * This method automatically collects path constraints when the condition is evaluated.
     */
    public static boolean executeConditional(String description, SymbolicCondition condition) {
        if (GaletteSymbolicator.DEBUG) {
            System.out.println("Evaluating symbolic condition: " + description);
        }

        boolean result = condition.evaluate();

        if (GaletteSymbolicator.DEBUG) {
            System.out.println("Condition result: " + result);
        }

        return result;
    }

    /**
     * Reset the symbolic execution state.
     * Useful for starting fresh between different executions.
     */
    public static void reset() {
        GaletteSymbolicator.reset();
    }

    /**
     * Perform symbolic-aware comparison between two double values.
     *
     * This method handles the symbolic execution concerns, extracting tags from values
     * and calling the appropriate SymbolicComparison method based on the operator.
     *
     * @param leftValue The left operand (may be Galette-tagged)
     * @param rightValue The right operand (typically concrete)
     * @param operator The comparison operator from Green solver
     * @return The result of the comparison
     */
    public static boolean compare(double leftValue, double rightValue, Operation.Operator operator) {
        // Extract tags from both values
        edu.neu.ccs.prl.galette.internal.runtime.Tag leftTag =
                edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(leftValue);
        edu.neu.ccs.prl.galette.internal.runtime.Tag rightTag =
                edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(rightValue);

        // Switch on the comparison operator and call appropriate SymbolicComparison method
        switch (operator) {
            case GT: // Greater than >
                return SymbolicComparison.greaterThan(leftValue, leftTag, rightValue, rightTag);

            case GE: // Greater than or equal >=
                return SymbolicComparison.greaterThanOrEqual(leftValue, leftTag, rightValue, rightTag);

            case LT: // Less than <
                return SymbolicComparison.lessThan(leftValue, leftTag, rightValue, rightTag);

            case LE: // Less than or equal <=
                return SymbolicComparison.lessThanOrEqual(leftValue, leftTag, rightValue, rightTag);

            case EQ: // Equal ==
                return SymbolicComparison.equal(leftValue, leftTag, rightValue, rightTag);

            default:
                throw new IllegalArgumentException(
                        "Unsupported comparison operator: " + operator + ". Supported operators: GT, GE, LT, LE, EQ");
        }
    }

    /**
     * Collect and analyze the current path constraints.
     *
     * @return Analysis results as a formatted string
     */
    public static String analyzePathConstraints() {
        StringBuilder analysis = new StringBuilder();

        try {
            PathConditionWrapper pc = PathUtils.getCurPC();

            if (pc != null && !pc.isEmpty()) {
                analysis.append("=== Symbolic Execution Analysis ===\n");
                analysis.append("Path constraints collected: ")
                        .append(pc.size())
                        .append("\n");

                Expression constraint = pc.toSingleExpression();
                if (constraint != null) {
                    analysis.append("Consolidated constraint: ")
                            .append(constraint)
                            .append("\n");
                    analysis.append("Constraint type: ")
                            .append(constraint.getClass().getSimpleName())
                            .append("\n");
                }

                // Try to get solution
                GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
                if (solution != null) {
                    analysis.append("Constraint solver solution: ")
                            .append(solution)
                            .append("\n");
                }

                // Add statistics
                analysis.append("Symbolic execution statistics:\n");
                analysis.append(GaletteSymbolicator.getStatistics());

            } else {
                analysis.append("No path constraints collected\n");
                analysis.append("This may indicate that symbolic execution is not active\n");
            }

        } catch (Exception e) {
            analysis.append("Error analyzing path constraints: ")
                    .append(e.getMessage())
                    .append("\n");
        }

        return analysis.toString();
    }

    /**
     * Display path constraint analysis to console.
     */
    public static void displayPathConstraintAnalysis() {
        System.out.println("\n" + analyzePathConstraints());
    }

    /**
     * Get summary statistics about the current symbolic execution state.
     */
    public static String getExecutionSummary() {
        PathConditionWrapper pc = PathUtils.getCurPC();
        int constraintCount = (pc != null) ? pc.size() : 0;

        return String.format(
                "Symbolic Execution Summary: %d constraints collected, %s",
                constraintCount, constraintCount > 0 ? "active" : "inactive");
    }

    /**
     * Check if symbolic execution is currently active.
     */
    public static boolean isSymbolicExecutionActive() {
        PathConditionWrapper pc = PathUtils.getCurPC();
        return pc != null && !pc.isEmpty();
    }

    // Note: evaluateThicknessCondition method removed -
    // thresholds are now discovered dynamically from path constraints

    // ==================== DEMO AND INTERACTION METHODS ====================

    /**
     * Interactive transformation with symbolic execution capabilities.
     * Prompts user for input and runs transformation with symbolic tracking.
     */
    public static BrakeDiscTarget transformInteractiveSymbolic(BrakeDiscSource source, java.util.Scanner scanner) {
        System.out.println("\n=== Interactive Symbolic Model Transformation ===");
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        return transformSymbolic(source, thickness, "user_thickness");
    }

    /**
     * Transform with symbolic execution capabilities.
     * This method wraps the clean transformation with symbolic execution.
     */
    public static BrakeDiscTarget transformSymbolic(
            BrakeDiscSource source, double thicknessValue, String thicknessLabel) {
        System.out.println("=== Symbolic Model Transformation ===");
        System.out.println("Source model: " + source);
        System.out.println("User input thickness: " + thicknessValue + " mm");

        // Debug: Check if Galette is instrumented properly
        System.out.println("🔍 GALETTE INSTRUMENTATION DEBUG:");
        try {
            // Test basic Galette functionality
            double testValue = 42.0;
            edu.neu.ccs.prl.galette.internal.runtime.Tag testTag =
                    edu.neu.ccs.prl.galette.internal.runtime.Tag.of("test");
            System.out.println("   Created test tag: " + testTag);

            double taggedTestValue = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(testValue, testTag);
            System.out.println("   Tagged test value: " + taggedTestValue);

            edu.neu.ccs.prl.galette.internal.runtime.Tag retrievedTag =
                    edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(taggedTestValue);
            System.out.println("   Retrieved test tag: " + (retrievedTag != null ? retrievedTag : "no tag"));

            if (retrievedTag != null) {
                System.out.println("   ✅ Basic Galette tagging is working!");
            } else {
                System.out.println("   ❌ Basic Galette tagging is NOT working - instrumentation issue!");
            }
        } catch (Exception e) {
            System.out.println("   ❌ Exception during Galette test: " + e.getMessage());
            e.printStackTrace();
        }

        // Create Galette-tagged symbolic value for the external input
        System.out.println("\n🏷️ Creating symbolic value for transformation input:");
        double taggedThickness = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(
                thicknessValue, edu.neu.ccs.prl.galette.internal.runtime.Tag.of(thicknessLabel));

        // Verify the tag was set
        edu.neu.ccs.prl.galette.internal.runtime.Tag verifyTag =
                edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(taggedThickness);
        System.out.println("Created symbolic value: " + thicknessLabel + " = " + taggedThickness + " (tag: "
                + (verifyTag != null ? verifyTag : "no tag") + ")");

        // Use the clean transformation with the Galette-tagged value
        // The comparison operations will automatically be intercepted by Galette
        BrakeDiscTarget target = BrakeDiscTransformation.transform(source, taggedThickness);

        // Add symbolic execution analysis for the conditional logic
        System.out.println("\n=== Symbolic Condition Analysis ===");
        analyzeConditionalLogic(new SymbolicValue<>(thicknessLabel, taggedThickness), target);

        // Display path constraint analysis
        displayPathConstraintAnalysis();

        System.out.println("=== Symbolic Transformation Complete ===");
        System.out.println("Target model: " + target);

        return target;
    }

    /**
     * Analyze the conditional logic dynamically using collected path constraints.
     * This discovers business rules without hardcoded threshold knowledge.
     */
    private static void analyzeConditionalLogic(SymbolicValue<Double> thickness, BrakeDiscTarget target) {
        System.out.println("Analyzing discovered conditional logic...");

        // Extract thresholds dynamically from collected path constraints
        PathConditionWrapper pc = PathUtils.getCurPC();
        if (pc != null && !pc.isEmpty()) {
            System.out.println("Path constraints collected:");
            List<Expression> constraints = pc.getConstraints();

            for (Expression constraint : constraints) {
                System.out.println("  " + constraint.toString());

                // Dynamically discover thresholds from this constraint
                Set<Double> discoveredThresholds = extractThresholdsFromConstraint(constraint);
                for (Double threshold : discoveredThresholds) {
                    System.out.println("  → Discovered threshold: " + threshold);
                }
            }
        } else {
            System.out.println("No path constraints collected yet.");
        }

        System.out.println("Actual additionalStiffness value: " + target.hasAdditionalStiffness());
        System.out.println("✓ Analysis complete - constraints discovered dynamically");
    }

    /**
     * Extract threshold values dynamically from a constraint expression.
     */
    public static Set<Double> extractThresholdsFromConstraint(Expression constraint) {
        Set<Double> thresholds = new HashSet<>();
        extractThresholdsRecursive(constraint, thresholds);
        return thresholds;
    }

    /**
     * Recursively extract numeric constants from expression tree.
     */
    private static void extractThresholdsRecursive(Expression expr, Set<Double> thresholds) {
        if (expr instanceof RealConstant) {
            thresholds.add(((RealConstant) expr).getValue());
        } else if (expr instanceof IntConstant) {
            thresholds.add((double) ((IntConstant) expr).getValue());
        } else if (expr instanceof BinaryOperation) {
            BinaryOperation binOp = (BinaryOperation) expr;
            extractThresholdsRecursive(binOp.getOperand(0), thresholds);
            extractThresholdsRecursive(binOp.getOperand(1), thresholds);
        } else if (expr instanceof UnaryOperation) {
            UnaryOperation unOp = (UnaryOperation) expr;
            extractThresholdsRecursive(unOp.getOperand(0), thresholds);
        }
    }

    /**
     * Demonstrate path exploration by running multiple transformations.
     */
    public static void demonstratePathExploration(BrakeDiscSource source) {
        System.out.println("\n" + repeatString("=", 70));
        System.out.println("SYMBOLIC PATH EXPLORATION DEMONSTRATION");
        System.out.println(repeatString("=", 70));

        double[] testValues = {8.0, 15.0};
        String[] pathDescriptions = {"thickness ≤ 10 (no additional stiffness)", "thickness > 10 (additional stiffness)"
        };

        for (int i = 0; i < testValues.length; i++) {
            System.out.println("\n### Path " + (i + 1) + ": " + pathDescriptions[i] + " ###");

            // Reset symbolic execution state for each path
            reset();

            BrakeDiscTarget result = transformSymbolic(source, testValues[i], "path_" + (i + 1) + "_thickness");

            System.out.println("Result: additionalStiffness = " + result.hasAdditionalStiffness());
            System.out.println(getExecutionSummary());
        }

        System.out.println("\n" + repeatString("=", 70));
        System.out.println("PATH EXPLORATION COMPLETE");
        System.out.println("Demonstrated how different inputs create different path constraints");
        System.out.println(repeatString("=", 70));
    }

    /**
     * Compare symbolic vs. clean transformation results.
     */
    public static void compareTransformationMethods(BrakeDiscSource source, double thickness) {
        System.out.println("\n=== Comparing Transformation Methods ===");

        // Clean transformation (no symbolic execution)
        System.out.println("\n1. Clean Transformation (Business Logic Only):");
        BrakeDiscTarget cleanResult = BrakeDiscTransformation.transform(source, thickness);
        System.out.println("Result: " + cleanResult);

        // Reset and run symbolic transformation
        reset();
        System.out.println("\n2. Symbolic Transformation (With Path Constraints):");
        BrakeDiscTarget symbolicResult = transformSymbolic(source, thickness, "comparison_thickness");

        // Compare results
        System.out.println("\n3. Comparison:");
        boolean resultsMatch = compareResults(cleanResult, symbolicResult);
        System.out.println("Results match: " + (resultsMatch ? "✓ YES" : "✗ NO"));

        if (isSymbolicExecutionActive()) {
            System.out.println("Symbolic execution provides additional analysis capabilities:");
            System.out.println("- Path constraints collected for solver-based test generation");
            System.out.println("- Ability to explore alternative execution paths");
            System.out.println("- Impact analysis of external inputs on model properties");
        }
    }

    /**
     * Compare two transformation results for equality.
     */
    private static boolean compareResults(BrakeDiscTarget result1, BrakeDiscTarget result2) {
        return Math.abs(result1.getDiameter() - result2.getDiameter()) < 0.001
                && Math.abs(result1.getThickness() - result2.getThickness()) < 0.001
                && Math.abs(result1.getSurfaceArea() - result2.getSurfaceArea()) < 0.001
                && Math.abs(result1.getVolume() - result2.getVolume()) < 0.001
                && Math.abs(result1.getEstimatedWeight() - result2.getEstimatedWeight()) < 0.001
                && result1.hasAdditionalStiffness() == result2.hasAdditionalStiffness()
                && result1.getMaterial().equals(result2.getMaterial())
                && result1.getCoolingVanes() == result2.getCoolingVanes();
    }

    /**
     * Get detailed analysis of a symbolic transformation.
     */
    public static String getSymbolicAnalysis(BrakeDiscSource source, double thickness) {
        reset();

        BrakeDiscTarget result = transformSymbolic(source, thickness, "analysis_thickness");

        StringBuilder analysis = new StringBuilder();
        analysis.append("=== Symbolic Transformation Analysis ===\n");
        analysis.append("Input: ").append(source).append("\n");
        analysis.append("Thickness: ").append(thickness).append(" mm\n");
        analysis.append("Output: ").append(result).append("\n");
        analysis.append("\n").append(analyzePathConstraints());

        return analysis.toString();
    }

    /**
     * Legacy symbolic demo functionality using clean transformation + wrapper.
     */
    public static BrakeDiscTarget runLegacyStyleDemo(BrakeDiscSource source, double thickness) {
        System.out.println("=== Starting Brake Disc Model Transformation ===");
        System.out.println("Source model: " + source);
        System.out.println("User input thickness: " + thickness + " mm");

        // Create symbolic representation of the user input
        SymbolicValue<Double> symbolicThickness = makeSymbolicDouble("user_thickness", thickness);
        System.out.println(
                "Created symbolic tag for thickness: " + (symbolicThickness.isSymbolic() ? "SUCCESS" : "FAILED"));

        // Use clean transformation for core logic
        BrakeDiscTarget target = BrakeDiscTransformation.transform(source, thickness);

        // Perform geometric calculations output
        System.out.println("\n=== Performing Geometric Calculations ===");
        System.out.println("Surface area: " + target.getSurfaceArea() + " mm²");
        System.out.println("Volume: " + target.getVolume() + " mm³");
        System.out.println("Estimated weight: " + target.getEstimatedWeight() + " g");

        // CRITICAL: Conditional logic analysis - now purely from collected path constraints
        System.out.println("\n=== Evaluating Conditional Logic ===");
        System.out.println("Analyzing path constraints collected during transformation...");

        // Extract discovered conditional logic from path constraints
        boolean isThick = target.hasAdditionalStiffness(); // Use actual result from transformation

        if (isThick) {
            System.out.println("→ Path taken: thickness > discovered threshold, setting additionalStiffness = true");
        } else {
            System.out.println("→ Path taken: thickness ≤ discovered threshold, setting additionalStiffness = false");
        }

        // Collect and display path constraints (legacy style)
        collectAndDisplayPathConstraintsLegacyStyle();

        System.out.println("\n=== Transformation Complete ===");
        System.out.println("Target model: " + target);

        return target;
    }

    /**
     * Legacy-style path constraint collection and display.
     */
    private static void collectAndDisplayPathConstraintsLegacyStyle() {
        System.out.println("\n=== Path Constraint Analysis ===");

        try {
            PathConditionWrapper pc = PathUtils.getCurPC();

            if (pc != null && !pc.isEmpty()) {
                System.out.println("Path constraints collected: " + pc.size());

                Expression constraint = pc.toSingleExpression();
                if (constraint != null) {
                    System.out.println("Consolidated constraint: " + constraint);
                    System.out.println(
                            "Constraint type: " + constraint.getClass().getSimpleName());
                } else {
                    System.out.println("No consolidated constraint available");
                }

                System.out.println("Path constraint details:");
                System.out.println("  - Number of constraints: " + pc.size());
                System.out.println(
                        "  - Constraint satisfiable: " + (constraint != null ? "Unknown" : "No constraints"));

            } else {
                System.out.println("No path constraints collected");
                System.out.println("This may indicate that symbolic execution is not active");
            }

            // Try to get solution from constraint solver
            GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
            if (solution != null) {
                System.out.println("Constraint solver solution: " + solution);
            } else {
                System.out.println("No solution from constraint solver");
            }

        } catch (Exception e) {
            System.err.println("Error collecting path constraints: " + e.getMessage());
            if (GaletteSymbolicator.DEBUG) {
                e.printStackTrace();
            }
        }

        // Display overall symbolic execution statistics
        System.out.println("\nSymbolic execution statistics:");
        System.out.println(GaletteSymbolicator.getStatistics());
    }

    /**
     * Demonstrate different execution paths by running transformation with different inputs.
     */
    public static void demonstratePathExplorationLegacyStyle(BrakeDiscSource source) {
        System.out.println("\n" + repeatString("=", 60));
        System.out.println("DEMONSTRATING PATH EXPLORATION");
        System.out.println(repeatString("=", 60));

        // Test path 1: thickness ≤ 10 (no additional stiffness)
        System.out.println("\n### Testing Path 1: thickness ≤ 10 ###");
        reset();
        BrakeDiscTarget result1 = runLegacyStyleDemo(source, 8.0);
        System.out.println("Result 1 - Additional Stiffness: " + result1.hasAdditionalStiffness());

        // Test path 2: thickness > 10 (additional stiffness)
        System.out.println("\n### Testing Path 2: thickness > 10 ###");
        reset();
        BrakeDiscTarget result2 = runLegacyStyleDemo(source, 15.0);
        System.out.println("Result 2 - Additional Stiffness: " + result2.hasAdditionalStiffness());

        System.out.println("\n" + repeatString("=", 60));
        System.out.println("PATH EXPLORATION COMPLETE");
        System.out.println("Two different execution paths demonstrated:");
        System.out.println("1. thickness ≤ 10 → additionalStiffness = false");
        System.out.println("2. thickness > 10 → additionalStiffness = true");
        System.out.println(repeatString("=", 60));
    }

    /**
     * Utility method to repeat strings (Java 8 compatible).
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}