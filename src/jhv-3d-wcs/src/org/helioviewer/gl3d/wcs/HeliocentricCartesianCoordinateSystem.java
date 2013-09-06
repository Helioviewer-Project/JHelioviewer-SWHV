package org.helioviewer.gl3d.wcs;

public class HeliocentricCartesianCoordinateSystem extends Cartesian3DCoordinateSystem {

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof HEEQCoordinateSystem) {
            return new HeliocentricCartesianToHEEQConversion(this, (HEEQCoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }
}
