package org.helioviewer.gl3d;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SphericalToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;
import org.junit.Assert;
import org.junit.Test;

public class SphericalToSolarSphereConversionTest {

    @Test
    public void testConversion() {
        SphericalCoordinateSystem sphericalCS = new SphericalCoordinateSystem();
        SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();

        SphericalToSolarSphereConversion conversion = (SphericalToSolarSphereConversion) sphericalCS.getConversion(solarSphereCS);

        // Test Center
        CoordinateVector spherical = sphericalCS.createCoordinateVector(0, Math.PI / 2);
        CoordinateVector ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Right
        spherical = sphericalCS.createCoordinateVector(Math.PI / 2, Math.PI / 2);
        ss = conversion.convert(spherical);
        Assert.assertEquals(Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Left
        spherical = sphericalCS.createCoordinateVector(-Math.PI / 2, Math.PI / 2);
        ss = conversion.convert(spherical);
        Assert.assertEquals(-Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Top
        spherical = sphericalCS.createCoordinateVector(0, 0);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Top (but adjusted)
        spherical = sphericalCS.createCoordinateVector(-Math.PI / 2, 0);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
        spherical = sphericalCS.createCoordinateVector(Math.PI / 2, 0);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Bottom
        spherical = sphericalCS.createCoordinateVector(0, Math.PI);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(-Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
        spherical = sphericalCS.createCoordinateVector(-Math.PI / 2, Math.PI);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(-Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
        spherical = sphericalCS.createCoordinateVector(Math.PI / 2, Math.PI);
        ss = conversion.convert(spherical);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(-Constants.SunRadius, ss.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0, ss.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
    }

}
