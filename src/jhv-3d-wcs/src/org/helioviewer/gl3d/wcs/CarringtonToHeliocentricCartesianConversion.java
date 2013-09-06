package org.helioviewer.gl3d.wcs;

public class CarringtonToHeliocentricCartesianConversion implements CoordinateConversion {
    private CarringtonCoordinateSystem carrington;
    private HeliocentricCartesianCoordinateSystem viewSpace;
    private StonyhurstCoordinateSystem stonyhurst;
    private CoordinateConversion carringtonToStonyhurst;
    private CoordinateConversion stonyhurstToHC;

    public CarringtonToHeliocentricCartesianConversion(CarringtonCoordinateSystem carrington, HeliocentricCartesianCoordinateSystem viewSpace) {
        this.carrington = carrington;
        this.viewSpace = viewSpace;
        this.stonyhurst = new StonyhurstCoordinateSystem(carrington.getB0());

        this.carringtonToStonyhurst = this.carrington.getConversion(this.stonyhurst);
        this.stonyhurstToHC = this.stonyhurst.getConversion(this.viewSpace);
    }

    public CoordinateVector convert(CoordinateVector vector) {
        return this.stonyhurstToHC.convert(this.carringtonToStonyhurst.convert(vector));

        /*
         * The uncommented Conversion was suggested by Marc deRosa, but did not
         * work as expected, thus the conversion takes the detour over
         * Stonyhurst coordinates
         */
        // double L = 0;
        // double B = 0;
        // L = this.carrington.getL0();
        // B = this.carrington.getB0();
        //
        // double r = vector.getValue(CarringtonCoordinateSystem.RADIUS);
        // double phi = vector.getValue(CarringtonCoordinateSystem.LONGITUDE);
        // double theta = vector.getValue(CarringtonCoordinateSystem.LATITUDE);
        //
        // double x = r*Math.sin(theta)*Math.sin(phi-L);
        // double y = r*Math.sin(theta)*Math.cos(phi-L)*Math.cos(B) +
        // r*Math.cos(theta)*Math.sin(B);
        // double z = - r*Math.sin(theta)*Math.cos(phi-L)*Math.sin(B) +
        // r*Math.cos(theta)*Math.cos(B);
        //
        // //X-Z forms the image plane, x pointing westwards and z northwards, y
        // towards the observer
        // return this.viewSpace.createCoordinateVector(x, z, y);
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.carrington;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.viewSpace;
    }
}
