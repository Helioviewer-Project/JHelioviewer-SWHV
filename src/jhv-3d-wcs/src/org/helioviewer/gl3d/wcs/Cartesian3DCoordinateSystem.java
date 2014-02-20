package org.helioviewer.gl3d.wcs;

/**
 * A generic 3-dimensional cartesian {@link CoordinateSystem}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class Cartesian3DCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    public static final int X_COORDINATE = 0;
    public static final int Y_COORDINATE = 1;
    public static final int Z_COORDINATE = 2;

    private CoordinateDimension xDimension;
    private CoordinateDimension yDimension;
    private CoordinateDimension zDimension;

    public Cartesian3DCoordinateSystem() {
        this(Unit.Kilometer);
    }

    public Cartesian3DCoordinateSystem(Unit unit) {
        this.xDimension = new GenericCoordinateDimension(unit, "X");
        this.yDimension = new GenericCoordinateDimension(unit, "Y");
        this.zDimension = new GenericCoordinateDimension(unit, "Z");
    }

    public int getDimensions() {
        return 3;
    }

    public CoordinateDimension getDimension(int dimension) {
        switch (dimension) {
        case X_COORDINATE:
            return this.xDimension;
        case Y_COORDINATE:
            return this.yDimension;
        case Z_COORDINATE:
            return this.zDimension;
        default:
            throw new IllegalArgumentException("Illegal dimension Number " + dimension + " for Coordinate System " + this);
        }
    }

}
