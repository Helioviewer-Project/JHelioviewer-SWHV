package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public final class ImageBounds {

    public static double radial(MetaData metaData) {
        ImageGeometry geometry = metaData.getGeometry();
        Region region = geometry.bounds;
        WcsHeader wcsHeader = geometry.wcs;
        if (!wcsHeader.projection.isSurfaceMap()) {
            double x0 = 0, x1 = geometry.grid.width();
            double y0 = 0, y1 = geometry.grid.height();
            double xm = 0.5 * (x0 + x1);
            double ym = 0.5 * (y0 + y1);
            double distance = metaData.getViewpoint().distance;
            double radius = radialBound(geometry, distance, x0, y0);
            radius = Math.max(radius, radialBound(geometry, distance, x1, y0));
            radius = Math.max(radius, radialBound(geometry, distance, x0, y1));
            radius = Math.max(radius, radialBound(geometry, distance, x1, y1));
            radius = Math.max(radius, radialBound(geometry, distance, xm, y0));
            radius = Math.max(radius, radialBound(geometry, distance, xm, y1));
            radius = Math.max(radius, radialBound(geometry, distance, x0, ym));
            radius = Math.max(radius, radialBound(geometry, distance, x1, ym));
            if (radius > 0)
                return radius;
        }

        Vec2 sun = sunCenter(metaData);
        double x0 = region.llx - sun.x;
        double x1 = region.urx - sun.x;
        double y0 = region.lly - sun.y;
        double y1 = region.ury - sun.y;
        return Math.max(
                Math.max(Math.hypot(x0, y0), Math.hypot(x1, y0)),
                Math.max(Math.hypot(x0, y1), Math.hypot(x1, y1)));
    }

    private static double radialBound(ImageGeometry geometry, double distance, double x, double y) {
        Vec2 plane = geometry.toPlane(x, y);
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(geometry.wcs, plane.x, plane.y);
        Vec2 hpcPlane = WcsProjection.helioprojectiveToHpcPlane(distance, helioprojective.x, helioprojective.y);
        if (hpcPlane != null && Double.isFinite(hpcPlane.x) && Double.isFinite(hpcPlane.y))
            return Math.hypot(hpcPlane.x, hpcPlane.y);
        return 0;
    }

    private static Vec2 sunCenter(MetaData metaData) {
        WcsHeader wcsHeader = metaData.getGeometry().wcs;
        if (!wcsHeader.projection.isSurfaceMap()) {
            Vec2 sun = WcsProjection.helioprojectiveToPlane(wcsHeader, 0, 0);
            if (sun != null && Double.isFinite(sun.x) && Double.isFinite(sun.y)) {
                ImageGeometry geometry = metaData.getGeometry();
                Vec2 pixel = geometry.toPixel(sun.x, sun.y);
                return new Vec2((pixel.x - geometry.referencePixel.x) * geometry.pixelSizeX,
                        -(pixel.y - geometry.referencePixel.y) * geometry.pixelSizeY);
            }
        }
        return wcsHeader.crval;
    }

    public static Region hpc(MetaData metaData) {
        ImageGeometry geometry = metaData.getGeometry();
        double x0 = 0, x1 = geometry.grid.width();
        double y0 = 0, y1 = geometry.grid.height();
        double xm = 0.5 * (x0 + x1);
        double ym = 0.5 * (y0 + y1);
        double[] bounds = {
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        updateHpcBounds(bounds, geometry, x0, y0);
        updateHpcBounds(bounds, geometry, x1, y0);
        updateHpcBounds(bounds, geometry, x0, y1);
        updateHpcBounds(bounds, geometry, x1, y1);
        updateHpcBounds(bounds, geometry, xm, y0);
        updateHpcBounds(bounds, geometry, xm, y1);
        updateHpcBounds(bounds, geometry, x0, ym);
        updateHpcBounds(bounds, geometry, x1, ym);
        return regionFromBounds(bounds);
    }

    private static void updateHpcBounds(double[] bounds, ImageGeometry geometry, double x, double y) {
        Vec2 plane = geometry.toPlane(x, y);
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(geometry.wcs, plane.x, plane.y);
        double hpcX = Math.toDegrees(helioprojective.x);
        double hpcY = Math.toDegrees(helioprojective.y);
        bounds[0] = Math.min(bounds[0], hpcX);
        bounds[1] = Math.max(bounds[1], hpcX);
        bounds[2] = Math.min(bounds[2], hpcY);
        bounds[3] = Math.max(bounds[3], hpcY);
    }

    private static Region regionFromBounds(double[] bounds) {
        return new Region(bounds[0], bounds[2],
                Math.max(Math.nextUp(0.0), bounds[1] - bounds[0]),
                Math.max(Math.nextUp(0.0), bounds[3] - bounds[2]));
    }

    private ImageBounds() {}
}
