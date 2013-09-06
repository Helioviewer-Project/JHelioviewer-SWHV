package org.helioviewer.gl3d.wcs;

public class StonyhurstToHEEQConversion implements CoordinateConversion {
    private StonyhurstCoordinateSystem stonyhurst;
    private HEEQCoordinateSystem heeq;

    public StonyhurstToHEEQConversion(StonyhurstCoordinateSystem stonyhurst, HEEQCoordinateSystem heeq) {
        this.heeq = heeq;
        this.stonyhurst = stonyhurst;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double r = vector.getValue(StonyhurstCoordinateSystem.RADIUS);
        double theta = vector.getValue(StonyhurstCoordinateSystem.LATITUDE);
        double phi = vector.getValue(StonyhurstCoordinateSystem.LONGITUDE);

        double xheeq = r * Math.cos(theta) * Math.cos(phi);
        double yheeq = r * Math.cos(theta) * Math.sin(phi);
        double zheeq = r * Math.sin(theta);

        CoordinateVector v = this.heeq.createCoordinateVector(xheeq, yheeq, zheeq);

        return v;
    }

    public StonyhurstCoordinateSystem getSourceCoordinateSystem() {
        return this.stonyhurst;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.heeq;
    }

}
