package org.helioviewer.jhv.metadata;

import java.nio.file.Files;
import java.nio.file.Path;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.WcsSolution;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WcsInterpreterTest {
    private static void near(double actual, double expected, double tolerance, String context) {
        if (!(Math.abs(actual - expected) <= tolerance))
            throw new AssertionError(context + ": " + actual + " != " + expected);
    }

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        // Frame 0, captured 2026-10-17 from jpip.swhv.oma.be/movies/
        // SOHO_LASCO_C3_white-light_F2026-10-14T23.46.45Z_T2026-10-16T23.46.45ZB1800L.jpx.
        // Keep the original XML: these legacy labels and uppercase units are
        // absent from the standard FITS fixtures used by the metadata comparisons.
        Path fixture = Path.of(args[0]);
        String xml = Files.readString(fixture);
        XMLMetaDataContainer input = new XMLMetaDataContainer(xml);
        if (!input.getRequiredString("CTYPE1").equals("SOLAR-X") ||
                !input.getRequiredString("CTYPE2").equals("SOLAR-Y"))
            throw new AssertionError("Legacy LASCO fixture lost its original axis labels");
        FitsMetaData metadata = new FitsMetaData(input, fixture.toUri());
        ImageGeometry geometry = metadata.getGeometry();
        WcsSolution wcs = geometry.wcs;
        if (wcs.projection != WcsSolution.Projection.TAN)
            throw new AssertionError("Legacy solar axes were not interpreted as TAN");

        double radiansPerPixel = 56 * Math.PI / (180 * 3600);
        near(geometry.referencePixel.x, 518.7, 1e-12, "FITS-to-buffer reference X");
        near(geometry.referencePixel.y, 491, 1e-12, "FITS-to-buffer reference Y");
        near(geometry.pixelToPlane.m00 / wcs.unitsPerRad, radiansPerPixel, 1e-16, "angular X scale");
        near(geometry.pixelToPlane.m11 / wcs.unitsPerRad, -radiansPerPixel, 1e-16, "angular Y scale");
        // Helioviewer's LASCO pixels are already derotated. The differing CROTA,
        // CROTA1 and CROTA2 values must not rotate the effective image geometry.
        near(geometry.pixelToPlane.m01, 0, 0, "LASCO matrix XY");
        near(geometry.pixelToPlane.m10, 0, 0, "LASCO matrix YX");

        for (double[] pixel : new double[][]{{.5, .5}, {512.5, 512.5}, {1023.5, 1023.5}, {518.7, 491}}) {
            // A central tangent plane has direction (x,y,1). Derive its angles
            // directly, without using another WCS implementation as the oracle.
            double x = (pixel[0] - 518.7) * radiansPerPixel;
            double y = (491 - pixel[1]) * radiansPerPixel;
            double longitude = Math.atan2(x, 1);
            double latitude = Math.atan2(y, Math.hypot(x, 1));
            Vec2 plane = geometry.toPlane(pixel[0], pixel[1]);
            double[] actual = wcs.planeToWorld(new double[]{plane.x, plane.y});
            near(actual[0], longitude, 2e-12, "legacy TAN longitude");
            near(actual[1], latitude, 2e-12, "legacy TAN latitude");
            Vec2 inverse = wcs.worldToPlane(longitude, latitude);
            Vec2 original = geometry.toPixel(inverse.x, inverse.y);
            near(original.x, pixel[0], 1e-9, "legacy TAN inverse X");
            near(original.y, pixel[1], 1e-9, "legacy TAN inverse Y");
        }

        // Recognizing this legacy convention must not restore the former fallback
        // which silently interpreted arbitrary unsupported projections as TAN.
        String unsupported = xml.replace("SOLAR-X", "HPLN-SIN").replace("SOLAR-Y", "HPLT-SIN");
        try {
            WcsInterpreter.read(new XMLMetaDataContainer(unsupported), 1024, 1024, 215);
            throw new AssertionError("Unsupported SIN projection was accepted");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("Unsupported WCS projection"))
                throw expected;
        }
        legacyCeaScales();
        instrumentFixtures(fixture.getParent().resolve("wcs-instruments"));
        System.out.println("PASS: original LASCO XML, legacy solar axes, angular scales, derotation and independent TAN geometry");
    }

    private static void legacyCeaScales() throws Exception {
        String prefix = "<xml><meta><CTYPE1>HPLN-CEA</CTYPE1><CTYPE2>HPLT-CEA</CTYPE2><CUNIT1>deg</CUNIT1>";
        String scales = "<CDELT1>-2</CDELT1><CDELT2>0.01</CDELT2>";
        String cd = "<CD1_1>-3</CD1_1><CD1_2>0.4</CD1_2><CD2_1>-0.02</CD2_1><CD2_2>0.03</CD2_2>";
        String pc = "<PC1_1>0.8</PC1_1><PC1_2>0.2</PC1_2><PC2_1>-0.3</PC2_1><PC2_2>0.7</PC2_2>";
        String[] cases = {scales, cd, scales + pc + cd, scales + "<CUNIT2>deg</CUNIT2>"};
        // FITS row scales times PC (or CD directly), with buffer Y reversed.
        // Missing CUNIT2 makes the legacy CEA row a dimensionless radian scale.
        double[][] expected = {
                {Math.toRadians(-2), 0, 0, -.01},
                {Math.toRadians(-3), Math.toRadians(-.4), -.02, -.03},
                {Math.toRadians(-1.6), Math.toRadians(.4), -.003, -.007},
                {Math.toRadians(-2), 0, 0, Math.toRadians(-.01)}
        };
        for (int i = 0; i < cases.length; i++) {
            WcsSolution wcs = WcsInterpreter.read(new XMLMetaDataContainer(prefix + cases[i] + "</meta></xml>"), 8, 8, 215);
            ImageGeometry geometry = new ImageGeometry(8, 8, wcs, false);
            double[] actual = {geometry.pixelToPlane.m00, geometry.pixelToPlane.m01,
                    geometry.pixelToPlane.m10, geometry.pixelToPlane.m11};
            for (int j = 0; j < actual.length; j++)
                near(actual[j], expected[i][j], 1e-15, "CEA row scale " + i + ":" + j);
        }
        // PC takes precedence over CD, so it still requires both CDELT values.
        for (String invalid : new String[]{pc + cd, "<CDELT1>-2</CDELT1>"}) {
            try {
                WcsInterpreter.read(new XMLMetaDataContainer(prefix + invalid + "</meta></xml>"), 8, 8, 215);
                throw new AssertionError("Missing CEA scales were accepted");
            } catch (IllegalArgumentException expectedFailure) {
                if (!expectedFailure.getMessage().contains("Image WCS requires"))
                    throw expectedFailure;
            }
        }
        System.out.println("PASS: independent CEA row scales, explicit units, PC/CD precedence and missing-scale rejection");
    }

    private static void instrumentFixtures(Path directory) throws Exception {
        // This original GONG header records neither CDELT nor CD. Keep the
        // loader's BasicMetaData fallback instead of inventing one arcsec/pixel.
        try {
            WcsInterpreter.read(new XMLMetaDataContainer(Files.readString(directory.resolve("gong-no-scale.xml"))), 2048, 2048, 215);
            throw new AssertionError("Missing image scales were accepted");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("Image WCS requires"))
                throw expected;
        }
        JSONArray cases = new JSONArray(Files.readString(directory.resolve("cases.json")));
        for (int i = 0; i < cases.length(); i++) {
            JSONObject expected = cases.getJSONObject(i);
            Path fixture = directory.resolve(expected.getString("file"));
            XMLMetaDataContainer input = new XMLMetaDataContainer(Files.readString(fixture));
            int width = expected.getJSONArray("size").getInt(0);
            int height = expected.getJSONArray("size").getInt(1);
            WcsSolution wcs = WcsInterpreter.read(input, width, height, 215);
            if (!wcs.projection.name().equals(expected.getString("projection")))
                throw new AssertionError("Unexpected projection: " + fixture);
            JSONArray matrix = expected.getJSONArray("matrixRadians");
            ImageGeometry unrotated = new ImageGeometry(width, height, wcs, false);
            double[] actual = {unrotated.pixelToPlane.m00, -unrotated.pixelToPlane.m01,
                    unrotated.pixelToPlane.m10, -unrotated.pixelToPlane.m11};
            for (int j = 0; j < 4; j++)
                near(actual[j] / wcs.unitsPerRad, matrix.getDouble(j), 1e-15, fixture + " angular matrix " + j);
            double longitude = expected.getJSONArray("referenceRadians").getDouble(0);
            double latitude = expected.getJSONArray("referenceRadians").getDouble(1);
            near(wcs.reference.x, longitude, 2e-14, fixture + " celestial reference X");
            near(wcs.reference.y, latitude, 2e-14, fixture + " celestial reference Y");
            double crpix1 = expected.getJSONArray("referencePixelFits").getDouble(0);
            double crpix2 = expected.getJSONArray("referencePixelFits").getDouble(1);
            near(unrotated.referencePixel.x + .5, crpix1, 1e-12, fixture + " FITS reference X");
            near(height - unrotated.referencePixel.y + .5, crpix2, 1e-12, fixture + " FITS reference Y");

            // Construct the celestial direction from a tangent/equidistant plane
            // and an orthonormal frame at the recorded reference. No WCS library
            // or adapter calculation supplies these expected angular coordinates.
            for (double[] pixel : new double[][]{{crpix1, crpix2}, {1, 1}, {width, height}}) {
                double dx = pixel[0] - crpix1, dy = pixel[1] - crpix2;
                double x = matrix.getDouble(0) * dx + matrix.getDouble(1) * dy;
                double y = matrix.getDouble(2) * dx + matrix.getDouble(3) * dy;
                double center = 1;
                if (wcs.projection == WcsSolution.Projection.ARC) {
                    double radius = Math.hypot(x, y);
                    double scale = radius == 0 ? 1 : Math.sin(radius) / radius;
                    center = Math.cos(radius);
                    x *= scale;
                    y *= scale;
                }
                double vx = Math.cos(longitude) * (Math.cos(latitude) * center - Math.sin(latitude) * y) - Math.sin(longitude) * x;
                double vy = Math.sin(longitude) * (Math.cos(latitude) * center - Math.sin(latitude) * y) + Math.cos(longitude) * x;
                double vz = Math.sin(latitude) * center + Math.cos(latitude) * y;
                double expectedLongitude = Math.atan2(vy, vx);
                double expectedLatitude = Math.atan2(vz, Math.hypot(vx, vy));
                Vec2 plane = unrotated.toPlane(pixel[0] - .5, height - (pixel[1] - .5));
                double[] angles = wcs.planeToWorld(new double[]{plane.x, plane.y});
                near(Math.IEEEremainder(angles[0] - expectedLongitude, 2 * Math.PI), 0, 2e-12, fixture + " longitude");
                near(angles[1], expectedLatitude, 2e-12, fixture + " latitude");
                Vec2 inverse = wcs.worldToPlane(expectedLongitude, expectedLatitude);
                near(inverse.x / wcs.unitsPerRad, plane.x / wcs.unitsPerRad, 2e-12, fixture + " inverse X");
                near(inverse.y / wcs.unitsPerRad, plane.y / wcs.unitsPerRad, 2e-12, fixture + " inverse Y");
            }

            // Also exercise the actual image metadata path, including retained
            // NAXIS=3 in the PUNCH JP2 header and the source-observer conversion.
            ImageGeometry geometry = new FitsMetaData(input, fixture.toUri()).getGeometry();
            if (geometry.grid.width() != width || geometry.grid.height() != height)
                throw new AssertionError("Wrong image extent: " + fixture);
            near(geometry.referencePixel.x, crpix1 - .5, 1e-12, fixture + " buffer reference X");
            near(geometry.referencePixel.y, height - crpix2 + .5, 1e-12, fixture + " buffer reference Y");
            System.out.println("PASS: captured " + expected.getString("instrument") + " metadata and independent " + wcs.projection + " geometry");
        }
    }
}
