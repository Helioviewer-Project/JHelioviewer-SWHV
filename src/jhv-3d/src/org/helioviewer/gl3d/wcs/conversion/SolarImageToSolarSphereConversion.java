package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.IllegalCoordinateVectorException;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;

public class SolarImageToSolarSphereConversion implements CoordinateConversion {

    private SolarImageCoordinateSystem solarImageCoordinateSystem;

    private SolarSphereCoordinateSystem solarSphereCoordinateSystem;

    private boolean autoAdjustToValidValue = false;

    public SolarImageToSolarSphereConversion(SolarImageCoordinateSystem solarImageCoordinateSystem, SolarSphereCoordinateSystem solarSphereCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.solarSphereCoordinateSystem = solarSphereCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return solarImageCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.solarSphereCoordinateSystem;
    }

    public boolean isAutoAdjustToValidValue() {
        return autoAdjustToValidValue;
    }

    public void setAutoAdjustToValidValue(boolean autoAdjustToValidValue) {
        this.autoAdjustToValidValue = autoAdjustToValidValue;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarImageCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

        double z = 0.0;
        if (this.solarImageCoordinateSystem.isInsideDisc(vector)) {
            z = extrudeZ(x, y);
        } else {
            if (this.isAutoAdjustToValidValue()) {
                double alpha = Math.atan(Math.abs(y) / Math.abs(x));
                if (x >= 0 && y >= 0) {// I. Quadrant
                    x = Math.cos(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                    y = Math.sin(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                } else if (x < 0 && y >= 0) {// II. Quadrant
                    x = -Math.cos(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                    y = Math.sin(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                } else if (x < 0 && y < 0) {// III. Quadrant
                    x = -Math.cos(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                    y = -Math.sin(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                } else {// VI. Quadrant
                    x = Math.cos(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                    y = -Math.sin(alpha) * this.solarImageCoordinateSystem.getSolarRadius();
                }
            } else {
                throw new IllegalCoordinateVectorException("Coordinates are not within solar disc, cannot convert to Spherical coordinates!");
            }
        }

        return this.solarSphereCoordinateSystem.createCoordinateVector(x, y, z);
    }

    private double extrudeZ(double x, double y) {
        double _x = x / this.solarImageCoordinateSystem.getSolarRadius();
        double _y = y / this.solarImageCoordinateSystem.getSolarRadius();
        double z = Math.sin(Math.acos(Math.sqrt(_x * _x + _y * _y))) * this.solarSphereCoordinateSystem.getSolarRadius();
        // double phi =
        // Math.acos(x/this.solarImageCoordinateSystem.getSolarRadius());
        // double theta =
        // Math.acos(y/this.solarImageCoordinateSystem.getSolarRadius());
        //
        // double z =
        // Math.sin(phi)*Math.sin(theta)*this.solarImageCoordinateSystem.getSolarRadius();
        // double z = 0.0;
        return z;
    }
}
