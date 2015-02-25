package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.IllegalCoordinateVectorException;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;

public class SolarImageToSphericalConversion implements CoordinateConversion {

    private SolarImageCoordinateSystem solarImageCoordinateSystem;

    private SphericalCoordinateSystem sphericalCoordinateSystem;

    private boolean autoAdjustToValidValue;

    public SolarImageToSphericalConversion(SolarImageCoordinateSystem solarImageCoordinateSystem, SphericalCoordinateSystem sphericalCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.sphericalCoordinateSystem = sphericalCoordinateSystem;
        this.autoAdjustToValidValue = false;
    }

    public boolean isAutoAdjustToValidValue() {
        return autoAdjustToValidValue;
    }

    public void setAutoAdjustToValidValue(boolean autoAdjustToValidValue) {
        this.autoAdjustToValidValue = autoAdjustToValidValue;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return solarImageCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.sphericalCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarImageCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

        double _x = x / solarImageCoordinateSystem.getSolarRadius();
        double _y = y / solarImageCoordinateSystem.getSolarRadius();

        double theta = Math.acos(_y);
        double phi = 0.0;
        if (!this.solarImageCoordinateSystem.isInsideDisc(vector)) {
            if (!this.autoAdjustToValidValue) {
                throw new IllegalCoordinateVectorException("Coordinates are not within solar disc, cannot convert to Spherical coordinates!");
            } else {
                if (x < 0)
                    phi = -Math.PI / 2;
                else if (x > 0)
                    phi = Math.PI / 2;
            }
        } else {
            if (theta == 0)
                phi = Math.acos(_x);
            else
                phi = -Math.acos(_x / Math.sin(theta)) + Math.PI / 2;
        }

        if (_x == 0) {
            if (x < 0)
                phi = -Math.PI / 2;
            else if (x > 0)
                phi = Math.PI / 2;
            else
                phi = 0;
        }

        return this.sphericalCoordinateSystem.createCoordinateVector(phi, theta);
    }

}
