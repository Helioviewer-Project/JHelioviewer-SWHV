package org.helioviewer.gl3d.wcs;

/**
 * A standard implementation of a {@link CoordinateDimension}.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GenericCoordinateDimension implements CoordinateDimension {
    private Unit unit;

    private String description;

    private double minValue;
    private double maxValue;

    public GenericCoordinateDimension(Unit unit) {
        this(unit, "Generic Unspecified Dimension");
    }

    public GenericCoordinateDimension(Unit unit, String description) {
        this(unit, description, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public GenericCoordinateDimension(Unit unit, String description, double minValue, double maxValue) {
        this.unit = unit;
        this.description = description;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getDescription() {
        return this.description;
    }

    public double getMinValue() {
        return this.minValue;
    }

    public double getMaxValue() {
        return this.maxValue;
    }

    public Unit getUnit() {
        return this.unit;
    }

}
