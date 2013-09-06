package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;

public class SolarSphereToSolarImageConversion implements CoordinateConversion {

    private SolarImageCoordinateSystem solarImageCoordinateSystem;

    private SolarSphereCoordinateSystem solarSphereCoordinateSystem;

    public SolarSphereToSolarImageConversion(SolarSphereCoordinateSystem solarSphereCoordinateSystem, SolarImageCoordinateSystem solarImageCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.solarSphereCoordinateSystem = solarSphereCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return solarSphereCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.solarImageCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarSphereCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarSphereCoordinateSystem.Y_COORDINATE);

        return this.solarImageCoordinateSystem.createCoordinateVector(x, y);
    }
}
