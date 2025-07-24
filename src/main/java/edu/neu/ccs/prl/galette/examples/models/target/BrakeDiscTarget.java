package edu.neu.ccs.prl.galette.examples.models.target;

/**
 * Target model representing an enhanced brake disc with computed geometric properties.
 *
 * This model contains derived attributes calculated during transformation,
 * including geometric measurements and engineering properties that depend
 * on external input (e.g., thickness provided by user).
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class BrakeDiscTarget {

    // Original properties from source model
    private double diameter;
    private String material;
    private int coolingVanes;

    // Computed geometric properties (derived during transformation)
    private double thickness;
    private double surfaceArea;
    private double volume;
    private double estimatedWeight;

    // Engineering properties based on conditional logic
    private boolean additionalStiffness;

    /**
     * Create a new brake disc target model.
     */
    public BrakeDiscTarget() {
        // Initialize with default values
        this.additionalStiffness = false;
    }

    /**
     * Get the diameter of the brake disc.
     *
     * @return Diameter in millimeters
     */
    public double getDiameter() {
        return diameter;
    }

    /**
     * Set the diameter of the brake disc.
     *
     * @param diameter Diameter in millimeters
     */
    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    /**
     * Get the material of the brake disc.
     *
     * @return Material type
     */
    public String getMaterial() {
        return material;
    }

    /**
     * Set the material of the brake disc.
     *
     * @param material Material type
     */
    public void setMaterial(String material) {
        this.material = material;
    }

    /**
     * Get the number of cooling vanes.
     *
     * @return Number of cooling vanes
     */
    public int getCoolingVanes() {
        return coolingVanes;
    }

    /**
     * Set the number of cooling vanes.
     *
     * @param coolingVanes Number of cooling vanes
     */
    public void setCoolingVanes(int coolingVanes) {
        this.coolingVanes = coolingVanes;
    }

    /**
     * Get the thickness of the brake disc.
     *
     * @return Thickness in millimeters
     */
    public double getThickness() {
        return thickness;
    }

    /**
     * Set the thickness of the brake disc.
     *
     * @param thickness Thickness in millimeters
     */
    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    /**
     * Get the surface area of the brake disc.
     *
     * @return Surface area in square millimeters
     */
    public double getSurfaceArea() {
        return surfaceArea;
    }

    /**
     * Set the surface area of the brake disc.
     *
     * @param surfaceArea Surface area in square millimeters
     */
    public void setSurfaceArea(double surfaceArea) {
        this.surfaceArea = surfaceArea;
    }

    /**
     * Get the volume of the brake disc.
     *
     * @return Volume in cubic millimeters
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Set the volume of the brake disc.
     *
     * @param volume Volume in cubic millimeters
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * Get the estimated weight of the brake disc.
     *
     * @return Estimated weight in grams
     */
    public double getEstimatedWeight() {
        return estimatedWeight;
    }

    /**
     * Set the estimated weight of the brake disc.
     *
     * @param estimatedWeight Estimated weight in grams
     */
    public void setEstimatedWeight(double estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }

    /**
     * Check if the brake disc has additional stiffness.
     *
     * This property is determined by conditional logic during transformation:
     * if thickness > 10mm, then additionalStiffness = true
     *
     * @return true if additional stiffness is present
     */
    public boolean hasAdditionalStiffness() {
        return additionalStiffness;
    }

    /**
     * Set whether the brake disc has additional stiffness.
     *
     * @param additionalStiffness true if additional stiffness is present
     */
    public void setAdditionalStiffness(boolean additionalStiffness) {
        this.additionalStiffness = additionalStiffness;
    }

    @Override
    public String toString() {
        return String.format(
                "BrakeDiscTarget{diameter=%.1fmm, material='%s', coolingVanes=%d, "
                        + "thickness=%.1fmm, surfaceArea=%.1fmm², volume=%.1fmm³, "
                        + "estimatedWeight=%.1fg, additionalStiffness=%s}",
                diameter, material, coolingVanes, thickness, surfaceArea, volume, estimatedWeight, additionalStiffness);
    }

    /**
     * Get a summary of the geometric properties.
     *
     * @return Formatted string with geometric information
     */
    public String getGeometricSummary() {
        return String.format(
                "Geometric Properties:\n" + "  Diameter: %.1f mm\n"
                        + "  Thickness: %.1f mm\n"
                        + "  Surface Area: %.1f mm²\n"
                        + "  Volume: %.1f mm³\n"
                        + "  Estimated Weight: %.1f g\n"
                        + "  Additional Stiffness: %s",
                diameter, thickness, surfaceArea, volume, estimatedWeight, additionalStiffness ? "Yes" : "No");
    }
}
