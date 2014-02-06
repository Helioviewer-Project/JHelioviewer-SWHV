package org.helioviewer.gl3d;

import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.IllegalCoordinateVectorException;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.junit.Assert;
import org.junit.Test;

public class SolarImageToSolarSphereConversionTest {

    @Test
    public void testConversion() {
        SolarImageCoordinateSystem solarDiskCS = new SolarImageCoordinateSystem();
        SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();

        SolarImageToSolarSphereConversion conversion = (SolarImageToSolarSphereConversion) solarDiskCS.getConversion(solarSphereCS);

        // Test Center
        CoordinateVector sd = solarDiskCS.createCoordinateVector(0, 0);
        CoordinateVector sphere = conversion.convert(sd);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
                
        // Test Left
        sd = solarDiskCS.createCoordinateVector(-solarSphereCS.getSolarRadius(), 0);
        sphere = conversion.convert(sd);
        Assert.assertEquals(-solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Test Right
        sd = solarDiskCS.createCoordinateVector(solarSphereCS.getSolarRadius(), 0);
        sphere = conversion.convert(sd);
        Assert.assertEquals(solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Test Up
        sd = solarDiskCS.createCoordinateVector(0, solarSphereCS.getSolarRadius());
        sphere = conversion.convert(sd);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        // Test Down
        sd = solarDiskCS.createCoordinateVector(0, -solarSphereCS.getSolarRadius());
        sphere = conversion.convert(sd);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(-solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);

        try {
            sd = solarDiskCS.createCoordinateVector(0, -solarSphereCS.getSolarRadius() * 3);
            sphere = conversion.convert(sd);
            Assert.fail();
        } catch (IllegalCoordinateVectorException e) {
        }

        conversion.setAutoAdjustToValidValue(true);
        sd = solarDiskCS.createCoordinateVector(0, -solarSphereCS.getSolarRadius() * 3);
        sphere = conversion.convert(sd);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.X_COORDINATE), 0.001);
        Assert.assertEquals(-solarSphereCS.getSolarRadius(), sphere.getValue(SolarSphereCoordinateSystem.Y_COORDINATE), 0.001);
        Assert.assertEquals(0.0, sphere.getValue(SolarSphereCoordinateSystem.Z_COORDINATE), 0.001);
    }

}
