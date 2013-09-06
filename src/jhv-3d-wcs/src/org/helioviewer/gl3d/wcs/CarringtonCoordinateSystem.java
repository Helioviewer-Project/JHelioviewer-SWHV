package org.helioviewer.gl3d.wcs;

import java.util.Date;

public class CarringtonCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    public static final int LONGITUDE = 0;
    public static final int LATITUDE = 1;
    public static final int RADIUS = 2;

    private CoordinateDimension longitude;
    private CoordinateDimension latitude;
    private CoordinateDimension radius;

    private Date observationDate;
    private double b0;
    private double l0;

    public CarringtonCoordinateSystem(Date observationDate) {
        this();
        this.observationDate = observationDate;
    }

    public CarringtonCoordinateSystem(double b0, double l0) {
        this();
        this.b0 = b0;
        this.l0 = l0;
    }

    private CarringtonCoordinateSystem() {
        this.longitude = new GenericCoordinateDimension(Unit.Degree, "Lon");
        this.latitude = new GenericCoordinateDimension(Unit.Degree, "Lat");
        this.radius = new GenericCoordinateDimension(Unit.Meter, "R");
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HEEQCoordinateSystem) {
            return new CarringtonToHEEQConversion(this, (HEEQCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem instanceof HeliocentricCartesianCoordinateSystem) {
            return new CarringtonToHeliocentricCartesianConversion(this, (HeliocentricCartesianCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem instanceof StonyhurstCoordinateSystem) {
            return new CarringtonToStonyhurstConversion(this, (StonyhurstCoordinateSystem) coordinateSystem);
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

    public double getB0() {
        return b0;
    }

    public double getL0() {
        return l0;
    }

    public int getDimensions() {
        return 3;
    }

    public Date getObservationDate() {
        return this.observationDate;
    }
}
