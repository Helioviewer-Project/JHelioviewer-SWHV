package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;

public class SphericalToSolarSphereConversion implements CoordinateConversion {

    private SphericalCoordinateSystem sphericalCoordinateSystem;

    private SolarSphereCoordinateSystem solarSphereCoordinateSystem;

    public SphericalToSolarSphereConversion(SphericalCoordinateSystem sphericalCoordinateSystem, SolarSphereCoordinateSystem solarSphereCoordinateSystem) {
        this.sphericalCoordinateSystem = sphericalCoordinateSystem;
        this.solarSphereCoordinateSystem = solarSphereCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return sphericalCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.solarSphereCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double phi = vector.getValue(SphericalCoordinateSystem.PHI);
        double theta = vector.getValue(SphericalCoordinateSystem.THETA);

        double sinTheta = Math.sin(theta);

        double x = this.solarSphereCoordinateSystem.getSolarRadius() * sinTheta * Math.sin(phi);
        double y = this.solarSphereCoordinateSystem.getSolarRadius() * Math.cos(theta);
        double z = this.solarSphereCoordinateSystem.getSolarRadius() * Math.cos(phi) * sinTheta;

        return this.solarSphereCoordinateSystem.createCoordinateVector(x, y, z);
    }
}
