package org.helioviewer.gl3d.wcs;

public class CarringtonToHEEQConversion implements CoordinateConversion {
    private CarringtonCoordinateSystem carrington;
    private HEEQCoordinateSystem heeq;

    public CarringtonToHEEQConversion(CarringtonCoordinateSystem carrington, HEEQCoordinateSystem heeq) {
        this.heeq = heeq;
        this.carrington = carrington;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double r = vector.getValue(CarringtonCoordinateSystem.RADIUS);
        double theta = vector.getValue(CarringtonCoordinateSystem.LATITUDE);
        double phi = vector.getValue(CarringtonCoordinateSystem.LONGITUDE);

        phi = phi - this.carrington.getL0();
        double xheeq = r * Math.cos(theta) * Math.cos(phi);
        double yheeq = r * Math.cos(theta) * Math.sin(phi);
        double zheeq = r * Math.sin(theta);

        return this.heeq.createCoordinateVector(xheeq, yheeq, zheeq);
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.carrington;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.heeq;
    }

}
