package org.helioviewer.gl3d.wcs;

public class HEEQToHeliocentricCartesianConversion implements CoordinateConversion {
    private HEEQCoordinateSystem heeq;
    private HeliocentricCartesianCoordinateSystem target;

    public HEEQToHeliocentricCartesianConversion(HEEQCoordinateSystem heeq, HeliocentricCartesianCoordinateSystem target) {
        this.heeq = heeq;
        this.target = target;
    }

    public CoordinateVector convert(CoordinateVector vector) {

        double xheeq = vector.getValue(HEEQCoordinateSystem.X_COORDINATE);
        double yheeq = vector.getValue(HEEQCoordinateSystem.Y_COORDINATE);
        double zheeq = vector.getValue(HEEQCoordinateSystem.Z_COORDINATE);

        double x = yheeq;
        double y = zheeq * Math.cos(this.heeq.getB0()) - xheeq * Math.sin(this.heeq.getB0());
        double z = zheeq * Math.sin(this.heeq.getB0()) + xheeq * Math.cos(this.heeq.getB0());
        return this.target.createCoordinateVector(x, y, z);
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.heeq;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.target;
    }

}
