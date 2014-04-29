package org.helioviewer.gl3d.wcs;

public class HeliocentricCartesian2000CoordinateSystem extends Cartesian3DCoordinateSystem {

    public HeliocentricCartesian2000CoordinateSystem() {
        super();
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof TimedHeliocentricCartesianCoordinateSystem) {
            return new HeliocentricCartesian2000ToTimedHeliocentricCartesianConversion(this, (TimedHeliocentricCartesianCoordinateSystem) coordinateSystem);
        }
        return super.getConversion(coordinateSystem);
    }

}
