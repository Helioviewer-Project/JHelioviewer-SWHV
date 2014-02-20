package org.helioviewer.gl3d.wcs;

/**
 * A generic 2-dimensional cartesian {@link CoordinateSystem}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class Cartesian2DCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    public static final int X_COORDINATE = 0;
    public static final int Y_COORDINATE = 1;

    private CoordinateDimension xDimension;
    private CoordinateDimension yDimension;

    public Cartesian2DCoordinateSystem() {
        this(Unit.Kilometer);
    }

    public Cartesian2DCoordinateSystem(Unit unit) {
        this(new GenericCoordinateDimension(unit, "X"), new GenericCoordinateDimension(unit, "Y"));
    }

    public Cartesian2DCoordinateSystem(CoordinateDimension xDimension, CoordinateDimension yDimension) {
        this.xDimension = xDimension;
        this.yDimension = yDimension;
    }

    public int getDimensions() {
        return 2;
    }

    public CoordinateDimension getDimension(int dimension) {
        switch (dimension) {
        case X_COORDINATE:
            return this.xDimension;
        case Y_COORDINATE:
            return this.yDimension;
        default:
            throw new IllegalArgumentException("Illegal dimension Number " + dimension + " for Coordinate System " + this);
        }
    }

}
