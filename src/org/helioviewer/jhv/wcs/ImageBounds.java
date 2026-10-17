package org.helioviewer.jhv.wcs;

import java.util.Arrays;

import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.math.Vec2;

public final class ImageBounds {

    public static double radial(ImageGeometry geometry, double distance) {
        // CAR/CEA describe the unit solar surface, not an off-limb image plane.
        // Their angular extent and longitude reference cannot enlarge a radius in solar radii.
        if (geometry.wcs.projection.isSurfaceMap())
            return 1;

        Region region = geometry.bounds;
        double radius = 0;
        for (Vec2 helioprojective : boundary(geometry)) {
            Vec2 hpcPlane = ObserverGeometry.helioprojectiveToHpcPlane(distance, helioprojective.x, helioprojective.y);
            if (hpcPlane != null && Double.isFinite(hpcPlane.x) && Double.isFinite(hpcPlane.y))
                radius = Math.max(radius, Math.hypot(hpcPlane.x, hpcPlane.y));
        }
        if (radius > 0)
            return radius;

        // Sun offset from the reference pixel in the rectangle's frame: image axes, Y up.
        double sunX = (geometry.sunPixel.x - geometry.referencePixel.x) * geometry.pixelSizeX;
        double sunY = (geometry.referencePixel.y - geometry.sunPixel.y) * geometry.pixelSizeY;
        double x0 = region.llx - sunX;
        double x1 = region.urx - sunX;
        double y0 = region.lly - sunY;
        double y1 = region.ury - sunY;
        return Math.max(
                Math.max(Math.hypot(x0, y0), Math.hypot(x1, y0)),
                Math.max(Math.hypot(x0, y1), Math.hypot(x1, y1)));
    }

    public static Region hpc(ImageGeometry geometry, double distance) {
        if (geometry.wcs.projection.isSurfaceMap()) {
            double radius = Math.toDegrees(Math.asin(1 / distance));
            return new Region(-radius, -radius, 2 * radius, 2 * radius);
        }
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vec2 helioprojective : boundary(geometry)) {
            double x = Math.toDegrees(helioprojective.x);
            double y = Math.toDegrees(helioprojective.y);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        // A valid projection region can lie entirely inside an invalid perimeter
        // (for example a large ARC image). Use its full angular domain for fitting.
        if (!Double.isFinite(minX))
            return new Region(Math.toDegrees(geometry.wcs.reference.x) - 180, -90, 360, 180);
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    // Finite helioprojective samples from the corners and edge midpoints (zero to eight points).
    private static Vec2[] boundary(ImageGeometry geometry) {
        double width = geometry.grid.width(), height = geometry.grid.height();
        double[] planePoints = new double[16];
        int n = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1)
                    continue;
                Vec2 plane = geometry.toPlane(.5 * i * width, .5 * j * height);
                planePoints[n++] = plane.x;
                planePoints[n++] = plane.y;
            }
        }
        double[] angles = geometry.wcs.planeToWorld(planePoints);
        Vec2[] points = new Vec2[8];
        n = 0;
        for (int i = 0; i < angles.length; i += 2)
            if (Double.isFinite(angles[i]) && Double.isFinite(angles[i + 1]))
                points[n++] = new Vec2(angles[i], angles[i + 1]);
        return Arrays.copyOf(points, n);
    }

    private ImageBounds() {}
}
