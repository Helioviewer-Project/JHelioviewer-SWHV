package org.helioviewer.gl3d.wcs;

public class CarringtonToStonyhurstConversion implements CoordinateConversion {
    private CarringtonCoordinateSystem carrington;
    private StonyhurstCoordinateSystem stonyhurst;

    public CarringtonToStonyhurstConversion(CarringtonCoordinateSystem carrington, StonyhurstCoordinateSystem stonyhurst) {
        this.stonyhurst = stonyhurst;
        this.carrington = carrington;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double r = vector.getValue(CarringtonCoordinateSystem.RADIUS);
        double theta = vector.getValue(CarringtonCoordinateSystem.LATITUDE);
        double phi = vector.getValue(CarringtonCoordinateSystem.LONGITUDE);

        return this.stonyhurst.createCoordinateVector(phi - this.carrington.getL0(), theta, r);
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.carrington;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.stonyhurst;
    }

}
