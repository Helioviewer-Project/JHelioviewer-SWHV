package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;

public final class ImageBounds {

    private ImageBounds() {
    }

    public static Region hpc(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        WcsProjection.Context context = new WcsProjection.Context(metaData);
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
        updateHpcBounds(bounds, context, x0, y0);
        updateHpcBounds(bounds, context, x1, y0);
        updateHpcBounds(bounds, context, x0, y1);
        updateHpcBounds(bounds, context, x1, y1);
        updateHpcBounds(bounds, context, xm, y0);
        updateHpcBounds(bounds, context, xm, y1);
        updateHpcBounds(bounds, context, x0, ym);
        updateHpcBounds(bounds, context, x1, ym);
        return regionFromBounds(bounds);
    }

    private static void updateHpcBounds(double[] bounds, WcsProjection.Context context, double x, double y) {
        Vec2 helioprojective = WcsProjection.planeToHelioprojective(context, x, y);
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
}
