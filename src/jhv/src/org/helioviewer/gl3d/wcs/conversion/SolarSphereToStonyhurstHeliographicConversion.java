package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.StonyhurstHeliographicCoordinateSystem;

public class SolarSphereToStonyhurstHeliographicConversion implements CoordinateConversion {

    private StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem;

    private SolarSphereCoordinateSystem solarSphereCoordinateSystem;

    public SolarSphereToStonyhurstHeliographicConversion(SolarSphereCoordinateSystem solarSphereCoordinateSystem, StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem) {
        this.stonyhurstCoordinateSystem = stonyhurstCoordinateSystem;
        this.solarSphereCoordinateSystem = solarSphereCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return solarSphereCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.stonyhurstCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarSphereCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarSphereCoordinateSystem.Y_COORDINATE);
        double z = vector.getValue(SolarSphereCoordinateSystem.Z_COORDINATE);

        double _y = y / solarSphereCoordinateSystem.getSolarRadius();
        double _x = x / solarSphereCoordinateSystem.getSolarRadius();

        double theta = Math.acos(_y) - Math.PI / 2;
        double phi = Math.acos((_x / (theta % Math.PI == 0 ? 1 : Math.sin(theta))));

        double r = Math.sqrt(x * x + y * y + z * z);
        return stonyhurstCoordinateSystem.createCoordinateVector(phi, theta, r);
    }
}
