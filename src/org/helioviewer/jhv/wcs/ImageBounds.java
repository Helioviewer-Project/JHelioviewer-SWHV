package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public final class ImageBounds {

    public static double radial(MetaData metaData) {
        ImageGeometry geometry = metaData.getGeometry();
        Region region = geometry.bounds;
        if (!geometry.wcs.projection.isSurfaceMap()) {
            double distance = metaData.getViewpoint().distance;
            double radius = 0;
            for (Vec2 helioprojective : boundary(geometry)) {
                Vec2 hpcPlane = WcsProjection.helioprojectiveToHpcPlane(distance, helioprojective.x, helioprojective.y);
                if (hpcPlane != null && Double.isFinite(hpcPlane.x) && Double.isFinite(hpcPlane.y))
                    radius = Math.max(radius, Math.hypot(hpcPlane.x, hpcPlane.y));
            }
            if (radius > 0)
                return radius;
        }

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

    public static Region hpc(MetaData metaData) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vec2 helioprojective : boundary(metaData.getGeometry())) {
            double x = Math.toDegrees(helioprojective.x);
            double y = Math.toDegrees(helioprojective.y);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    // Helioprojective angles of the image corners and edge midpoints.
    private static Vec2[] boundary(ImageGeometry geometry) {
        double width = geometry.grid.width(), height = geometry.grid.height();
        Vec2[] points = new Vec2[8];
        int n = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1)
                    continue;
                Vec2 plane = geometry.toPlane(.5 * i * width, .5 * j * height);
                points[n++] = WcsProjection.planeToHelioprojective(geometry.wcs, plane.x, plane.y);
            }
        }
        return points;
    }

    private ImageBounds() {}
}
