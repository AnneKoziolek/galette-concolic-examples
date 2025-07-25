package edu.neu.ccs.prl.galette.examples;

import edu.neu.ccs.prl.galette.concolic.knarr.runtime.GaletteSymbolicator;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathConditionWrapper;
import edu.neu.ccs.prl.galette.concolic.knarr.runtime.PathUtils;
import edu.neu.ccs.prl.galette.examples.models.source.BrakeDiscSource;
import edu.neu.ccs.prl.galette.examples.models.target.BrakeDiscTarget;
import edu.neu.ccs.prl.galette.examples.transformation.BrakeDiscTransformation;
import edu.neu.ccs.prl.galette.knarr.SymbolicExecutionWrapper;
import edu.neu.ccs.prl.galette.internal.runtime.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import za.ac.sun.cs.green.expr.Expression;

/**
 * Main application demonstrating concolic execution in model transformations.
 *
 * This example shows how Galette can be used to track symbolic values through
 * model transformation code, specifically demonstrating:
 *
 * 1. External input as symbolic values
 * 2. Propagation of symbolic values through calculations
 * 3. Conditional logic creating different execution paths
 * 4. Path constraint collection for impact analysis
 *
 * The example implements the use case described in the migration goals email:
 * analyzing the impact of external inputs in model-driven engineering scenarios.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class ModelTransformationExample {

    /**
     * Helper method to repeat a string (Java 8 compatible).
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(repeatString("=", 80));
        System.out.println("GALETTE CONCOLIC EXECUTION DEMO: MODEL TRANSFORMATION");
        System.out.println(repeatString("=", 80));
        System.out.println();
        System.out.println("This example demonstrates how Galette can track symbolic values");
        System.out.println("through model transformations to analyze the impact of external inputs.");
        System.out.println();

        // Initialize the symbolic execution environment
        System.out.println("Initializing Galette symbolic execution environment...");
        
        // Debug: Check system property
        System.out.println("🔧 DEBUG: galette.concolic.interception.enabled = " + 
            System.getProperty("galette.concolic.interception.enabled"));
        System.out.println("🔧 DEBUG: Boolean.getBoolean result = " + 
            Boolean.getBoolean("galette.concolic.interception.enabled"));
            
        SymbolicExecutionWrapper.reset(); // Start with clean state

        // Create a sample brake disc source model
        BrakeDiscSource sourceModel = createSampleBrakeDisc();
        System.out.println("Created sample brake disc: " + sourceModel);
        System.out.println();

        // Show menu options
        showMenu();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        // just one iteration for now
        // while (running) {
        System.out.print("\nSelect an option (1-3): ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character
            System.out.println();

            switch (choice) {
                case 1:
                    runCleanTransformation(sourceModel, scanner);
                    break;
                case 2:
                    runConcolicPathExploration(sourceModel);
                    break;
                case 3:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please select 1-3.");
            }

            if (running) {
                System.out.println("\n" + repeatString("-", 60));
                showMenu();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            scanner.nextLine(); // Clear invalid input
            showMenu();
        }
        // }

        scanner.close();
    }

    /**
     * Display the main menu options.
     */
    private static void showMenu() {
        System.out.println("Available options:");
        System.out.println("1. Standard transformation (concrete execution, no path exploration)");
        System.out.println("2. Concolic execution with automated path constraint collection and exploration");
        System.out.println("3. Exit");
    }

    /**
     * Create a sample brake disc for demonstration.
     *
     * @return Sample brake disc source model
     */
    private static BrakeDiscSource createSampleBrakeDisc() {
        return new BrakeDiscSource(350.0, "cast iron", 24);
    }

    /**
     * Run clean transformation (business logic only).
     */
    private static void runCleanTransformation(BrakeDiscSource source, Scanner scanner) {
        System.out.println("=== CLEAN TRANSFORMATION (NO SYMBOLIC EXECUTION) ===");
        System.out.println();
        System.out.println("Source brake disc: " + source);
        System.out.print("Please enter the brake disc thickness (mm): ");

        double thickness = scanner.nextDouble();

        System.out.println("Transforming with thickness: " + thickness + " mm");
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, thickness);

        System.out.println("Transformation complete.");
        System.out.println("Additional stiffness: " + (result.hasAdditionalStiffness() ? "Yes" : "No"));

        System.out.println();
        System.out.println("=== TRANSFORMATION RESULTS ===");
        System.out.println(result.getGeometricSummary());
        System.out.println("\nNote: This version uses pure business logic without symbolic execution.");
    }

    /**
     * True concolic execution with automated path exploration.
     * This demonstrates the core Galette/Knarr functionality.
     */
    private static void runConcolicPathExploration(BrakeDiscSource source) {
        System.out.println("=== TRUE CONCOLIC EXECUTION WITH PATH EXPLORATION ===");
        System.out.println();
        System.out.println("This demonstrates proper concolic execution using Galette and Knarr:");
        System.out.println("1. Start with initial input and collect path constraints");
        System.out.println("2. Use constraint solver to generate inputs for unexplored paths");
        System.out.println("3. Automatically discover boundary conditions");
        System.out.println();

        performConcolicAnalysis(source);
    }

    /**
     * Perform true concolic execution analysis using Galette and Knarr.
     * This method implements the core concolic execution workflow:
     * 1. Execute with initial input and collect path constraints
     * 2. Use constraint solver to generate inputs for alternative paths
     * 3. Systematically explore all reachable execution paths
     */
    private static void performConcolicAnalysis(BrakeDiscSource source) {
        System.out.println(repeatString("=", 70));
        System.out.println("CONCOLIC EXECUTION ANALYSIS");
        System.out.println(repeatString("=", 70));

        List<Double> exploredInputs = new ArrayList<>();
        List<String> pathConstraints = new ArrayList<>();
        int maxIterations = 10; // Prevent infinite loops
        int iteration = 0;

        // Start with an initial input value
        double initialThickness = 12.0;

        System.out.println("\n=== ITERATION " + (++iteration) + ": Initial Execution ===");
        System.out.println("Starting concolic analysis with thickness = " + initialThickness + " mm");

        // Execute with initial input and collect path constraints
        ConcolicResult initialResult = executeConcolic(source, initialThickness, "thickness_" + iteration);
        exploredInputs.add(initialThickness);
        pathConstraints.add(initialResult.pathConstraint);

        System.out.println("Initial path constraint: " + initialResult.pathConstraint);
        System.out.println("Result: additionalStiffness = " + initialResult.result.hasAdditionalStiffness());

        // Use constraint solver to generate alternative inputs
        while (iteration < maxIterations) {
            System.out.println("\n=== Generating Alternative Inputs ===");

            // Try to generate input for the opposite path
            Double alternativeInput = generateAlternativeInput(exploredInputs, pathConstraints);

            if (alternativeInput == null) {
                System.out.println("No more alternative paths found. Exploration complete.");
                break;
            }

            // Skip if we've already explored this input
            if (exploredInputs.contains(alternativeInput)) {
                System.out.println("Input " + alternativeInput + " already explored, trying boundary analysis...");
                alternativeInput = exploreBoundaryConditions(exploredInputs);
                if (alternativeInput == null) break;
            }

            System.out.println("\n=== ITERATION " + (++iteration) + ": Alternative Path ===");
            System.out.println("Exploring with generated input: " + alternativeInput + " mm");

            ConcolicResult altResult = executeConcolic(source, alternativeInput, "thickness_" + iteration);
            exploredInputs.add(alternativeInput);
            pathConstraints.add(altResult.pathConstraint);

            System.out.println("Path constraint: " + altResult.pathConstraint);
            System.out.println("Result: additionalStiffness = " + altResult.result.hasAdditionalStiffness());

            // Check if we've found a different execution path
            if (!altResult.pathConstraint.equals(initialResult.pathConstraint)) {
                System.out.println("✓ NEW EXECUTION PATH DISCOVERED!");
            }
        }

        // Summary of concolic analysis
        System.out.println("\n" + repeatString("=", 70));
        System.out.println("CONCOLIC ANALYSIS SUMMARY");
        System.out.println(repeatString("=", 70));
        System.out.println("Total iterations: " + iteration);
        System.out.println("Inputs explored: " + exploredInputs.size());
        System.out.println("Unique path constraints: " + countUniqueConstraints(pathConstraints));

        System.out.println("\nExplored inputs and their path constraints:");
        for (int i = 0; i < exploredInputs.size(); i++) {
            System.out.println("  Input " + exploredInputs.get(i) + " mm → " + pathConstraints.get(i));
        }

        // Boundary analysis
        System.out.println("\n=== BOUNDARY CONDITION ANALYSIS ===");
        analyzeBoundaryConditions(exploredInputs);
    }

    /**
     * Container for concolic execution results.
     */
    private static class ConcolicResult {
        final BrakeDiscTarget result;
        final String pathConstraint;

        ConcolicResult(BrakeDiscTarget result, String pathConstraint, boolean hasConstraints) {
            this.result = result;
            this.pathConstraint = pathConstraint;
        }
    }

    /**
     * Execute transformation with concolic analysis.
     */
    private static ConcolicResult executeConcolic(BrakeDiscSource source, double thickness, String label) {
        // Reset symbolic execution state
        GaletteSymbolicator.reset();
        PathUtils.resetPC();

        // Create symbolic value for thickness - we need to get the TAGGED VALUE, not just the tag
        Tag symbolicTag = GaletteSymbolicator.makeSymbolicDouble(label, thickness);

        // The problem: makeSymbolicDouble() creates a tagged value internally but only returns the tag!
        // We need to manually create the tagged value and use that
        double taggedThickness = edu.neu.ccs.prl.galette.internal.runtime.Tainter.setTag(thickness, symbolicTag);

        // Verify the tag was applied
        Tag verifyTag = edu.neu.ccs.prl.galette.internal.runtime.Tainter.getTag(taggedThickness);
        System.out.println("Created symbolic value: " + label + " = " + thickness + " (tag: "
                + (verifyTag != null ? verifyTag : "no tag") + ")");

        // TEMPORARY: Direct comparison test to verify agent is working
        System.out.println("🔧 Testing direct comparison in ModelTransformationExample...");
        boolean testComparison = taggedThickness > 10.0;
        System.out.println("🔧 Direct comparison result: " + taggedThickness + " > 10.0 = " + testComparison);

        // Execute the transformation with the TAGGED value (this is the key fix!)
        System.out.println("🔧 About to call BrakeDiscTransformation.transform() with tagged thickness");
        BrakeDiscTarget result = BrakeDiscTransformation.transform(source, taggedThickness);
        System.out.println("🔧 BrakeDiscTransformation.transform() completed");

        // Collect path constraints
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        String constraintDescription = "no constraints";
        boolean hasConstraints = false;

        if (pc != null && !pc.isEmpty()) {
            hasConstraints = true;
            if (pc.toSingleExpression() != null) {
                constraintDescription = pc.toSingleExpression().toString();
            } else {
                constraintDescription = "constraints collected: " + pc.size();
            }
        }

        System.out.println("Path constraints: " + constraintDescription);

        return new ConcolicResult(result, constraintDescription, hasConstraints);
    }

    // Note: performSymbolicThicknessCheck method removed -
    // path constraints are now collected automatically during transformation execution

    /**
     * Generate alternative input values to explore different execution paths.
     */
    private static Double generateAlternativeInput(List<Double> exploredInputs, List<String> pathConstraints) {
        // Try to use the constraint solver to generate alternative inputs
        try {
            GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();
            if (solution != null) {
                // Try to extract thickness value from solution
                String solutionStr = solution.toString();
                if (solutionStr.contains("thickness")) {
                    // Simple parsing - in a real implementation, this would be more sophisticated
                    try {
                        // Look for numeric values in the solution
                        String[] parts = solutionStr.split("\\s+");
                        for (String part : parts) {
                            try {
                                double value = Double.parseDouble(part);
                                if (value > 0 && value < 100) { // Reasonable thickness range
                                    return value;
                                }
                            } catch (NumberFormatException ignored) {
                                // Continue searching
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Could not parse solver solution: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Constraint solver not available: " + e.getMessage());
        }

        // Fallback: Generate alternative inputs based on analysis of explored inputs
        // Use dynamic threshold discovery from path constraints
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        Set<Double> discoveredThresholds = new HashSet<>();
        if (pc != null && !pc.isEmpty()) {
            List<Expression> constraints = pc.getConstraints();
            for (Expression constraint : constraints) {
                discoveredThresholds.addAll(SymbolicExecutionWrapper.extractThresholdsFromConstraint(constraint));
            }
        }

        // If we found thresholds from constraints, use them
        if (!discoveredThresholds.isEmpty()) {
            for (Double threshold : discoveredThresholds) {
                boolean hasLowValue = exploredInputs.stream().anyMatch(v -> v <= threshold);
                boolean hasHighValue = exploredInputs.stream().anyMatch(v -> v > threshold);

                if (!hasLowValue) {
                    return threshold - 1.0; // Test the thickness <= threshold path
                } else if (!hasHighValue) {
                    return threshold + 1.0; // Test the thickness > threshold path
                }
            }
        }

        // Ultimate fallback: try values around input distribution
        if (!exploredInputs.isEmpty()) {
            Collections.sort(new ArrayList<>(exploredInputs));
            double minInput = Collections.min(exploredInputs);
            double maxInput = Collections.max(exploredInputs);
            double midpoint = (minInput + maxInput) / 2.0;

            boolean hasLowValue = exploredInputs.stream().anyMatch(v -> v <= midpoint);
            boolean hasHighValue = exploredInputs.stream().anyMatch(v -> v > midpoint);

            if (!hasLowValue) {
                return midpoint - 1.0;
            } else if (!hasHighValue) {
                return midpoint + 1.0;
            }
        }

        return null; // No more obvious alternatives
    }

    /**
     * Explore boundary conditions around discovered threshold values.
     * Uses dynamic threshold discovery instead of hardcoded values.
     */
    private static Double exploreBoundaryConditions(List<Double> exploredInputs) {
        // Discover thresholds dynamically from path constraints
        PathConditionWrapper pc = PathUtils.getCurPCWithGalette();
        Set<Double> discoveredThresholds = new HashSet<>();
        if (pc != null && !pc.isEmpty()) {
            List<Expression> constraints = pc.getConstraints();
            for (Expression constraint : constraints) {
                discoveredThresholds.addAll(SymbolicExecutionWrapper.extractThresholdsFromConstraint(constraint));
            }
        }

        // Generate boundary values around discovered thresholds
        for (Double threshold : discoveredThresholds) {
            double[] boundaryValues = {threshold - 0.1, threshold, threshold + 0.1, threshold - 0.01, threshold + 0.01};

            for (double value : boundaryValues) {
                if (!exploredInputs.contains(value)) {
                    System.out.println("Exploring boundary condition around threshold " + threshold + ": " + value);
                    return value;
                }
            }
        }

        // Fallback: explore around input distribution patterns
        if (!exploredInputs.isEmpty() && exploredInputs.size() > 1) {
            List<Double> sortedInputs = new ArrayList<>(exploredInputs);
            Collections.sort(sortedInputs);

            for (int i = 0; i < sortedInputs.size() - 1; i++) {
                double gap = sortedInputs.get(i + 1) - sortedInputs.get(i);
                if (gap > 1.0) { // Significant gap might indicate boundary
                    double boundaryValue = (sortedInputs.get(i) + sortedInputs.get(i + 1)) / 2.0;
                    if (!exploredInputs.contains(boundaryValue)) {
                        System.out.println("Exploring potential boundary: " + boundaryValue);
                        return boundaryValue;
                    }
                }
            }
        }

        return null; // No more boundary conditions to explore
    }

    /**
     * Count unique path constraints to understand path coverage.
     */
    private static int countUniqueConstraints(List<String> constraints) {
        return (int) constraints.stream().distinct().count();
    }

    /**
     * Analyze boundary conditions from explored inputs.
     * Let Galette/Knarr constraint solver determine thresholds dynamically.
     */
    private static void analyzeBoundaryConditions(List<Double> inputs) {
        System.out.println("=== Dynamic Boundary Analysis (using Galette/Knarr) ===");

        // Use Galette's constraint solver to analyze discovered thresholds
        // rather than hardcoded knowledge
        GaletteSymbolicator.InputSolution solution = GaletteSymbolicator.solvePathCondition();

        if (solution != null) {
            System.out.println("Constraint solver analysis:");
            System.out.println("  Solution variables: " + solution.getLabels());
            System.out.println("  Constraint solution: " + solution);

            // Extract threshold information from solver solution
            for (String label : solution.getLabels()) {
                Object value = solution.getValue(label);
                if (value instanceof Number && label.contains("thickness")) {
                    double threshold = ((Number) value).doubleValue();
                    System.out.println("  → Discovered threshold from constraints: " + threshold + " mm");

                    // Analyze inputs around this discovered threshold
                    analyzeInputsAroundThreshold(inputs, threshold);
                }
            }
        } else {
            // Fallback: analyze input distribution patterns
            System.out.println("No constraint solution available, analyzing input patterns...");
            analyzeInputPatterns(inputs);
        }

        System.out.println("✓ Boundary analysis complete - using dynamic constraint discovery");
    }

    /**
     * Analyze inputs around a discovered threshold value.
     */
    private static void analyzeInputsAroundThreshold(List<Double> inputs, double threshold) {
        List<Double> belowThreshold = inputs.stream()
                .filter(v -> v <= threshold)
                .sorted()
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        List<Double> aboveThreshold = inputs.stream()
                .filter(v -> v > threshold)
                .sorted()
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        System.out.println("    Inputs ≤ " + threshold + ": " + belowThreshold);
        System.out.println("    Inputs > " + threshold + ": " + aboveThreshold);

        if (!belowThreshold.isEmpty() && !aboveThreshold.isEmpty()) {
            System.out.println("    ✓ Both execution paths explored");
        } else if (belowThreshold.isEmpty()) {
            System.out.println("    ⚠ Missing exploration of ≤ " + threshold + " path");
        } else if (aboveThreshold.isEmpty()) {
            System.out.println("    ⚠ Missing exploration of > " + threshold + " path");
        }
    }

    /**
     * Analyze input patterns when no constraints are available.
     */
    private static void analyzeInputPatterns(List<Double> inputs) {
        if (inputs.size() < 2) {
            System.out.println("  Insufficient inputs for pattern analysis");
            return;
        }

        List<Double> sortedInputs = new ArrayList<>(inputs);
        Collections.sort(sortedInputs);

        System.out.println("  Input distribution: " + sortedInputs);
        System.out.println("  Range: " + sortedInputs.get(0) + " to " + sortedInputs.get(sortedInputs.size() - 1));

        // Look for gaps that might indicate boundaries
        for (int i = 0; i < sortedInputs.size() - 1; i++) {
            double gap = sortedInputs.get(i + 1) - sortedInputs.get(i);
            if (gap > 2.0) { // Significant gap
                System.out.println("  → Potential boundary around "
                        + (sortedInputs.get(i) + sortedInputs.get(i + 1)) / 2.0 + " mm (gap: " + gap + ")");
            }
        }
    }
}
