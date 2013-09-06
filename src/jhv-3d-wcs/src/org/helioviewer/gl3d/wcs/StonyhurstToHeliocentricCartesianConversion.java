package org.helioviewer.gl3d.wcs;

public class StonyhurstToHeliocentricCartesianConversion implements CoordinateConversion {
    private StonyhurstCoordinateSystem stonyhurst;
    private HEEQCoordinateSystem heeq;
    private HeliocentricCartesianCoordinateSystem hc;

    public StonyhurstToHeliocentricCartesianConversion(StonyhurstCoordinateSystem stonyhurst, HeliocentricCartesianCoordinateSystem hc) {
        if (stonyhurst.getObservationDate() == null) {
            this.heeq = new HEEQCoordinateSystem(stonyhurst.getB0());
        } else {
            this.heeq = new HEEQCoordinateSystem(stonyhurst.getObservationDate());
        }
        this.stonyhurst = stonyhurst;
        this.hc = hc;
    }

    public CoordinateVector convert(CoordinateVector vector) {

        CoordinateConversion stonyhurstToHEEQ = this.stonyhurst.getConversion(heeq);
        CoordinateConversion heeqToHC = heeq.getConversion(hc);

        CoordinateVector v0 = stonyhurstToHEEQ.convert(vector);
        CoordinateVector v = heeqToHC.convert(v0);

        return v;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.stonyhurst;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.hc;
    }
}
