package org.helioviewer.gl3d.wcs;

/**
 * A {@link CoordinateVector} describes a point within its
 * {@link CoordinateSystem} by providing values in each dimension of its
 * {@link CoordinateSystem}. A {@link CoordinateVector} can be transformed to
 * another {@link CoordinateSystem} by using a {@link CoordinateConversion} of
 * its source coordinate system.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class CoordinateVector {
    private double[] coordinates;

    private CoordinateSystem coordinateSystem;

    protected CoordinateVector(CoordinateSystem coordinateSystem, double... value) {
        this.coordinateSystem = coordinateSystem;
        this.coordinates = value;
        if (!consistencyCheck()) {
            // Log.warn("CoordinateVector is inconsistent with bounds for CoordianteSystem "+this.coordinateSystem+" with values "+this.toString());
            // throw new
            // IllegalArgumentException("CoordinateVector cannot be built for CoordianteSystem "+this.coordinateSystem+" with values "+this.toString());
        }
    }

    private boolean consistencyCheck() {
        for (int i = 0; i < this.coordinates.length; i++) {
            double v = this.coordinates[i];
            if (v < this.coordinateSystem.getDimension(i).getMinValue() || v > this.coordinateSystem.getDimension(i).getMaxValue()) {
                return false;
            }
        }
        return true;
    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystem;
    }

    public double[] getValues() {
        return this.coordinates;
    }

    public double getValue(int dimension) {
        return this.coordinates[dimension];
    }

    public void setValue(int dimension, double value) {
        this.coordinates[dimension] = value;
    }

    public String toString() {
        String s = "";
        for (int d = 0; d < getCoordinateSystem().getDimensions(); d++) {
            s += this.coordinates[d] + " " + getCoordinateSystem().getDimension(d).getUnit().getAbbreviation();
            if ((d + 1) < getCoordinateSystem().getDimensions())
                s += ", ";
        }
        return "[" + s + "]";
    }
}
