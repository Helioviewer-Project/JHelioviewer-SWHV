package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public final class ImageBounds {

    public static double radial(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        Vec2 sun = sunCenter(metaData);
        double x0 = region.llx - sun.x;
        double x1 = region.urx - sun.x;
        double y0 = region.lly - sun.y;
        double y1 = region.ury - sun.y;
        return Math.max(
                Math.max(Math.hypot(x0, y0), Math.hypot(x1, y0)),
                Math.max(Math.hypot(x0, y1), Math.hypot(x1, y1)));
    }

    public static double inscribed(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        Vec2 sun = sunCenter(metaData);
        double right = region.urx - sun.x;
        double left = sun.x - region.llx;
        double top = region.ury - sun.y;
        double bottom = sun.y - region.lly;
        return Math.max(0, Math.min(Math.min(right, left), Math.min(top, bottom)));
    }

    private static Vec2 sunCenter(MetaData metaData) {
        WcsHeader wcsHeader = metaData.getWcsHeader();
        if (!wcsHeader.projection.isSurfaceMap()) {
            Vec2 sun = WcsProjection.helioprojectiveToPlane(wcsHeader, 0, 0);
            if (sun != null && Double.isFinite(sun.x) && Double.isFinite(sun.y))
                return sun;
        }
        return wcsHeader.crval;
    }

    public static Region hpc(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        WcsHeader wcsHeader = metaData.getWcsHeader();
        double x0 = region.llx;
        double x1 = region.urx;
        double y0 = region.lly;
        double y1 = region.ury;
        double xm = 0.5 * (x0 + x1);
        double ym = 0.5 * (y0 + y1);
        double[] bounds = {
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        updateHpcBounds(bounds, wcsHeader, x0, y0);
        updateHpcBounds(bounds, wcsHeader, x1, y0);
        updateHpcBounds(bounds, wcsHeader, x0, y1);
        updateHpcBounds(bounds, wcsHeader, x1, y1);
        updateHpcBounds(bounds, wcsHeader, xm, y0);
        updateHpcBounds(bounds, wcsHeader, xm, y1);
        updateHpcBounds(bounds, wcsHeader, x0, ym);
        updateHpcBounds(bounds, wcsHeader, x1, ym);
        return regionFromBounds(bounds);
    }

    private static void updateHpcBounds(double[] bounds, WcsHeader wcsHeader, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(wcsHeader, x, y);
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
