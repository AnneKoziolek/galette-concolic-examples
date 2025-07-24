package edu.neu.ccs.prl.galette.examples.models.source;

/**
 * Source model representing a simple brake disc with basic properties.
 *
 * This represents the initial model before transformation, containing
 * only fundamental properties that are typically known upfront.
 *
 * @author [Anne Koziolek](https://github.com/AnneKoziolek)
 */
public class BrakeDiscSource {

    /**
     * Diameter of the brake disc in millimeters.
     */
    private double diameter;

    /**
     * Material of the brake disc (e.g., "cast iron", "carbon ceramic").
     */
    private String material;

    /**
     * Number of cooling vanes (if any).
     */
    private int coolingVanes;

    /**
     * Create a new brake disc source model.
     *
     * @param diameter Diameter in millimeters
     * @param material Material type
     * @param coolingVanes Number of cooling vanes
     */
    public BrakeDiscSource(double diameter, String material, int coolingVanes) {
        this.diameter = diameter;
        this.material = material;
        this.coolingVanes = coolingVanes;
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

    @Override
    public String toString() {
        return String.format(
                "BrakeDiscSource{diameter=%.1fmm, material='%s', coolingVanes=%d}", diameter, material, coolingVanes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BrakeDiscSource that = (BrakeDiscSource) obj;
        return Double.compare(that.diameter, diameter) == 0
                && coolingVanes == that.coolingVanes
                && (material != null ? material.equals(that.material) : that.material == null);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(diameter);
        result = 31 * result + (material != null ? material.hashCode() : 0);
        result = 31 * result + coolingVanes;
        return result;
    }
}
