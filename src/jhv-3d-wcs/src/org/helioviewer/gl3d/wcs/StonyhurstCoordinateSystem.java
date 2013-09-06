package org.helioviewer.gl3d.wcs;

import java.util.Date;

public class StonyhurstCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    public static final int LONGITUDE = 0;
    public static final int LATITUDE = 1;
    public static final int RADIUS = 2;

    private CoordinateDimension longitude;
    private CoordinateDimension latitude;
    private CoordinateDimension radius;

    private Date observationDate;

    private double b0;

    public StonyhurstCoordinateSystem(Date observationDate) {
        this();
        this.observationDate = observationDate;
    }

    public StonyhurstCoordinateSystem(double b0) {
        this();
        this.b0 = b0;
    }

    public StonyhurstCoordinateSystem() {
        this.longitude = new GenericCoordinateDimension(Unit.Radian, "Lon");
        this.latitude = new GenericCoordinateDimension(Unit.Radian, "Lat");
        this.radius = new GenericCoordinateDimension(Unit.Meter, "R");
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HEEQCoordinateSystem) {
            return new StonyhurstToHEEQConversion(this, (HEEQCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem instanceof HeliocentricCartesianCoordinateSystem) {
            return new StonyhurstToHeliocentricCartesianConversion(this, (HeliocentricCartesianCoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }

    public CoordinateDimension getDimension(int dimension) {
        switch (dimension) {
        case LONGITUDE:
            return this.longitude;
        case LATITUDE:
            return this.latitude;
        case RADIUS:
            return this.radius;
        default:
            throw new IllegalArgumentException("Illegal dimension Number " + dimension + " for Coordinate System " + this);
        }
    }

    public int getDimensions() {
        return 3;
    }

    public Date getObservationDate() {
        return this.observationDate;
    }

    public double getB0() {
        return b0;
    }
}
