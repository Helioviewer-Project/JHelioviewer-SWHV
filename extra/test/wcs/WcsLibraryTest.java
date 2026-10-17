package org.helioviewer.jhv.wcs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.math.Vec2;

import nom.tam.fits.Header;

public final class WcsLibraryTest {
    private static Header header(WcsSolution.Projection projection, double longitude, double latitude) throws Exception {
        Header result = new Header();
        result.addValue("NAXIS", 2, null);
        result.addValue("CTYPE1", "HPLN-" + projection, null);
        result.addValue("CTYPE2", "HPLT-" + projection, null);
        result.addValue("CUNIT1", "deg", null);
        result.addValue("CUNIT2", "deg", null);
        result.addValue("CRVAL1", longitude, null);
        result.addValue("CRVAL2", latitude, null);
        if (projection == WcsSolution.Projection.ZPN)
            result.addValue("PV2_1", 1., null);
        if (projection == WcsSolution.Projection.CEA)
            result.addValue("PV2_1", .5, null);
        if (projection == WcsSolution.Projection.AZP) {
            result.addValue("PV2_1", 2., null);
            result.addValue("PV2_2", 20., null);
        }
        return result;
    }

    private static void near(double actual, double expected, String context) {
        if (!(Math.abs(actual - expected) < 2e-12))
            throw new AssertionError(context + ": " + actual + " != " + expected);
    }

    private static void projections() throws Exception {
        // Expected coordinates come from an orthonormal 3D frame and a ray/plane
        // intersection, not from WCSLIB, Astropy or JHV's projection implementation.
        for (WcsSolution.Projection projection : WcsSolution.Projection.values()) {
            for (double[] reference : new double[][]{{0, 0}, {35, 20}}) {
                WcsSolution wcs = new WcsSolution(header(projection, reference[0], reference[1]), 1);
                double a = Math.toRadians(reference[0]), b = Math.toRadians(reference[1]);
                for (double[] point : new double[][]{{10, 5}, {45, 30}, {30, -10}, {60, 20}}) {
                    double lon = Math.toRadians(point[0]), lat = Math.toRadians(point[1]);
                    double vx = Math.cos(lat) * Math.cos(lon), vy = Math.cos(lat) * Math.sin(lon), vz = Math.sin(lat);
                    double east = -Math.sin(a) * vx + Math.cos(a) * vy;
                    double north = -Math.sin(b) * (Math.cos(a) * vx + Math.sin(a) * vy) + Math.cos(b) * vz;
                    double center = Math.cos(b) * (Math.cos(a) * vx + Math.sin(a) * vy) + Math.sin(b) * vz;
                    double x, y;
                    if (projection == WcsSolution.Projection.CAR || projection == WcsSolution.Projection.CEA) {
                        x = Math.atan2(east, center);
                        y = projection == WcsSolution.Projection.CAR
                                ? Math.atan2(north, Math.hypot(east, center)) : north / .5;
                    } else if (projection == WcsSolution.Projection.TAN) {
                        x = east / center;
                        y = north / center;
                    } else if (projection == WcsSolution.Projection.AZP) {
                        double gamma = Math.toRadians(20);
                        // Eye=(0,0,-2), native point=(-north,east,center), tangent-plane
                        // origin=(0,0,1), normal=(sin(gamma),0,cos(gamma)).
                        double t = 3 * Math.cos(gamma) / (-north * Math.sin(gamma) + (center + 2) * Math.cos(gamma));
                        double hitX = -north * t, hitZ = -2 + t * (center + 2) - 1;
                        x = east * t;
                        y = -Math.cos(gamma) * hitX + Math.sin(gamma) * hitZ;
                    } else {
                        double r = Math.hypot(east, north);
                        double eta = Math.atan2(r, center);
                        x = eta * east / r;
                        y = eta * north / r;
                    }
                    Vec2 actual = wcs.worldToPlane(lon, lat);
                    if (actual == null)
                        throw new AssertionError("Valid point rejected: " + projection);
                    near(actual.x, x, projection + " forward X");
                    near(actual.y, y, projection + " forward Y");
                    double[] inverse = wcs.planeToWorld(new double[]{x, y});
                    near(Math.IEEEremainder(inverse[0] - lon, 2 * Math.PI), 0, projection + " inverse longitude");
                    near(inverse[1], lat, projection + " inverse latitude");
                }
            }
        }
    }

    private static void domains() throws Exception {
        WcsSolution tangent = new WcsSolution(header(WcsSolution.Projection.TAN, 0, 0), 1);
        near(tangent.planeToWorld(new double[]{-.1, 0})[0], -Math.atan(.1), "continuous observer longitude");
        for (WcsSolution.Projection projection : new WcsSolution.Projection[]{WcsSolution.Projection.CAR, WcsSolution.Projection.CEA}) {
            WcsSolution wcs = new WcsSolution(header(projection, 0, 0), 1);
            double[] points = wcs.planeToWorld(new double[]{4 * Math.PI, 0, -4 * Math.PI, 0, 0, 10});
            near(points[0], 0, "positive longitude copy");
            near(points[2], 0, "negative longitude copy");
            if (!Double.isNaN(points[4]) || !Double.isNaN(points[5]))
                throw new AssertionError("Invalid cylinder latitude accepted");
        }
        WcsSolution arc = new WcsSolution(header(WcsSolution.Projection.ARC, 0, 0), 1);
        if (!Double.isNaN(arc.planeToWorld(new double[]{Math.PI + .1, 0})[0]))
            throw new AssertionError("ARC beyond pi accepted");
        ImageGeometry largeArc = new ImageGeometry(10, 10, arc, new Mat2(1, 0, 0, -1), new Vec2(5, 5));
        Region bounds = ImageBounds.hpc(largeArc, 10);
        // The ARC disk of radius pi fits inside this square, whose eight sampled
        // boundary points are all invalid. Its celestial coverage is the full sky.
        near(bounds.llx, -180, "interior ARC longitude minimum");
        near(bounds.width, 360, "interior ARC longitude span");
        near(bounds.lly, -90, "interior ARC latitude minimum");
        near(bounds.height, 180, "interior ARC latitude span");
        Header zpnHeader = header(WcsSolution.Projection.ZPN, 0, 0);
        zpnHeader.addValue("PV2_0", .125, null);
        WcsSolution zpn = new WcsSolution(zpnHeader, 1);
        Vec2 pole = zpn.worldToPlane(0, 0);
        near(Math.hypot(pole.x, pole.y), .125, "ZPN pole radius");
        near(pole.x, .125 * zpn.nativePoles[0], "prepared ZPN pole X");
        near(pole.y, .125 * zpn.nativePoles[1], "prepared ZPN pole Y");
        if (!Double.isNaN(zpn.planeToWorld(new double[]{0, 0})[0]))
            throw new AssertionError("ZPN inverse hole accepted");
        for (double lambda : new double[]{0, -1, 1.1, 1e-60}) {
            Header invalid = header(WcsSolution.Projection.CEA, 0, 0);
            invalid.addValue("PV2_1", lambda, null);
            rejected(invalid);
        }
        zpnHeader.addValue("PV2_6", 1., null);
        rejected(zpnHeader);
        Header azp = header(WcsSolution.Projection.AZP, 0, 0);
        azp.addValue("PV2_1", -1., null);
        rejected(azp);
    }

    private static void coefficients() throws Exception {
        // Explicit expected families, independent of the adapter's uses_pv predicate.
        double[][] expected = {
                {0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0},
                {0, 2.123456789, 20.123456789, 0, 0, 0},
                {.004123456789, 1.123456789, .01, .02, .003, .0004},
                {0, 0, 0, 0, 0, 0}, {0, .7123456789, 0, 0, 0, 0}};
        for (WcsSolution.Projection projection : WcsSolution.Projection.values()) {
            Header input = header(projection, 0, 0);
            double[] values = expected[projection.ordinal()];
            for (int i = 0; i < values.length; i++)
                input.addValue("PV2_" + i, values[i] == 0 ? 42.123456789 : values[i], null);
            WcsSolution wcs = new WcsSolution(input, 1);
            if (wcs.projection != projection)
                throw new AssertionError("Native projection code changed: " + projection);
            for (int i = 0; i < values.length; i++)
                near(wcs.pv2[i], (float) values[i], projection + " effective PV2_" + i);
            if (projection == WcsSolution.Projection.CEA)
                near(wcs.worldToPlane(.1, .2).y, Math.sin(.2) / (float) values[1], "CEA uses encoded coefficient before native setup");
        }
    }

    private static void fitsGeometry() throws Exception {
        Header input = header(WcsSolution.Projection.TAN, 0, 0);
        input.addValue("CRPIX1", 2.25, null);
        input.addValue("CRPIX2", 4.75, null);
        input.addValue("CDELT1", .125, null);
        input.addValue("CDELT2", -.25, null);
        input.addValue("PC1_1", .8, null);
        input.addValue("PC1_2", -.3, null);
        input.addValue("PC2_1", .4, null);
        input.addValue("PC2_2", 1.1, null);
        WcsSolution wcs = new WcsSolution(input, 7);
        double scale = 7 * Math.PI / 180;
        for (boolean pixelsAlreadyRotated : new boolean[]{false, true}) {
            ImageGeometry geometry = new ImageGeometry(9, 7, wcs, pixelsAlreadyRotated);
            near(geometry.referencePixel.x, 1.75, "buffer reference X");
            near(geometry.referencePixel.y, 2.75, "buffer reference Y");
            // The declared FITS point (5.25,2.75) is three columns and two rows
            // below CRPIX. Solve its plane position directly from CDELT * PC.
            double x = pixelsAlreadyRotated ? 3 * Math.hypot(.1, -.1) : .375;
            double y = pixelsAlreadyRotated ? -2 * Math.hypot(-.0375, -.275) : .25;
            Vec2 plane = geometry.toPlane(4.75, 4.75);
            near(plane.x, x * scale, "FITS-to-buffer plane X, rotated " + pixelsAlreadyRotated);
            near(plane.y, y * scale, "FITS-to-buffer plane Y, rotated " + pixelsAlreadyRotated);
            Vec2 pixel = geometry.toPixel(x * scale, y * scale);
            near(pixel.x, 4.75, "buffer inverse X");
            near(pixel.y, 4.75, "buffer inverse Y");
            near(geometry.withSize(11, 8).referencePixel.y, 2.75, "source-size reconciliation preserves buffer reference");
        }
        // The buffer conversion, including the already-rotated policy, cannot
        // alter the native solution's original FITS matrix and reference pixel.
        near(wcs.referencePixel.y, 4.75, "native FITS reference unchanged");
        near(wcs.pixelToPlane.m01, -.0375 * scale, "native FITS shear unchanged");
        near(wcs.pixelToPlane.m11, -.275 * scale, "native FITS signed scale unchanged");
    }

    private static void rejected(Header header) {
        try {
            new WcsSolution(header, 1);
            throw new AssertionError("Invalid WCS accepted");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().startsWith("WCSLIB:"))
                throw expected;
        }
    }

    private static void adjustments() throws Exception {
        WcsSolution original = new WcsSolution(header(WcsSolution.Projection.CAR, 35, 20), 1);
        WcsSolution adjusted = original.adjusted(0, -40 * 3600);
        if (original.adjusted(0, 0) != original || original.adjusted(0, -40 * 3600) != adjusted)
            throw new AssertionError("Adjustment cache changed ownership");
        WcsSolution reference = new WcsSolution(header(WcsSolution.Projection.CAR, 35, -20), 1);
        Vec2 expected = reference.worldToPlane(.8, .4), actual = adjusted.worldToPlane(.8, .4);
        near(actual.x, expected.x, "adjusted default pole X");
        near(actual.y, expected.y, "adjusted default pole Y");
        near(original.reference.y, Math.toRadians(20), "original WCS mutated");
    }

    private static void concurrency() throws Exception {
        WcsSolution wcs = new WcsSolution(header(WcsSolution.Projection.TAN, 0, 0), 1);
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int i = 0; i < 8; i++)
                jobs.add(() -> {
                    for (int j = 0; j < 200; j++) {
                        Vec2 plane = wcs.worldToPlane(.1, .2);
                        double[] angles = wcs.planeToWorld(new double[]{plane.x, plane.y});
                        near(angles[0], .1, "concurrent longitude");
                        near(angles[1], .2, "concurrent latitude");
                    }
                    return null;
                });
            for (Future<Void> result : pool.invokeAll(jobs))
                result.get();
        }
    }

    public static void main(String[] args) throws Exception {
        projections();
        domains();
        coefficients();
        fitsGeometry();
        adjustments();
        concurrency();
        System.out.println("PASS: independent WCS geometry, inverse domains, parameter rejection, adjusted pole defaults and shared native ownership");
    }
}
