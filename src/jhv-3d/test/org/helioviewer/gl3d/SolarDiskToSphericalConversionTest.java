package org.helioviewer.gl3d;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSphericalConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SphericalCoordinateSystem;
import org.junit.Assert;
import org.junit.Test;

public class SolarDiskToSphericalConversionTest {
    @Test
    public void testAtan2() {
        double _x = 0.0;
        double _y = 0.0;
        double phi = Math.atan2(_x, _y) % (Math.PI);
        Assert.assertEquals(0.0, phi, 0.000);

        _x = 1.0;
        _y = 0.0;
        phi = Math.atan2(_x, _y) % (Math.PI);
        Assert.assertEquals(Math.PI / 2, phi, 0.000);

        _x = -1.0;
        _y = 0.0;
        phi = Math.atan2(_x, _y) % (Math.PI);
        Assert.assertEquals(-Math.PI / 2, phi, 0.000);

        _x = -0.5;
        _y = Math.PI / 2;
        phi = Math.atan2(_x, _y) % (Math.PI);
        Assert.assertTrue(-Math.PI / 2 < phi);
        Assert.assertTrue("Phi is wrong!" + phi, 0 > phi);
    }

    @Test
    public void testConversion() {
        SolarImageCoordinateSystem solarDiskCS = new SolarImageCoordinateSystem();
        SphericalCoordinateSystem sphericalCS = new SphericalCoordinateSystem();

        SolarImageToSphericalConversion conversion = (SolarImageToSphericalConversion) solarDiskCS.getConversion(sphericalCS);

        // Test Center
        CoordinateVector sd = solarDiskCS.createCoordinateVector(0, 0);
        CoordinateVector spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // Test Left Bound
        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius, 0);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(-Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius * 0.5, 0);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertTrue("Phi is Wrong: " + spherical.getValue(SphericalCoordinateSystem.PHI), -Math.PI / 2 < spherical.getValue(SphericalCoordinateSystem.PHI));
        Assert.assertTrue(0 > spherical.getValue(SphericalCoordinateSystem.PHI));

        // Test Right Bound
        sd = solarDiskCS.createCoordinateVector(Constants.SunRadius, 0);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // Test Upper Bound
        sd = solarDiskCS.createCoordinateVector(0, Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // Test Bottom Bound
        sd = solarDiskCS.createCoordinateVector(0, -Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // Test adjusted Coordintaes
        conversion.setAutoAdjustToValidValue(true);

        // BottomLeft
        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius, -Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(-Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // BottomRight
        sd = solarDiskCS.createCoordinateVector(Constants.SunRadius, -Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(Math.PI, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // TopLeft
        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius, Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(-Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // TopRight
        sd = solarDiskCS.createCoordinateVector(Constants.SunRadius, Constants.SunRadius);
        spherical = conversion.convert(sd);
        Assert.assertEquals(0, spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        // Partly intersects
        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius, Constants.SunRadius * 0.8);
        spherical = conversion.convert(sd);
        // Assert.assertEquals(Math.PI,
        // spherical.getValue(SphericalCoordinateSystem.THETA), 0.001);
        Assert.assertEquals(-Math.PI / 2, spherical.getValue(SphericalCoordinateSystem.PHI), 0.001);

        sd = solarDiskCS.createCoordinateVector(-Constants.SunRadius * 0.5, Constants.SunRadius * 0.4);
        spherical = conversion.convert(sd);
        Assert.assertTrue(-Math.PI / 2 < spherical.getValue(SphericalCoordinateSystem.PHI));

        sd = solarDiskCS.createCoordinateVector(Constants.SunRadius * 0.5, Constants.SunRadius * 0.4);
        spherical = conversion.convert(sd);
        Assert.assertTrue(Math.PI / 2 > spherical.getValue(SphericalCoordinateSystem.PHI));
    }

}
