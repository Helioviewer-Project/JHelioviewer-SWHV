package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;

public final class ImageBounds {

    private ImageBounds() {}

    public static double radial(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        Vec2 crval = metaData.getWcsHeader().crval;
        double x0 = region.llx - crval.x;
        double x1 = region.urx - crval.x;
        double y0 = region.lly - crval.y;
        double y1 = region.ury - crval.y;
        return Math.max(
                Math.max(Math.hypot(x0, y0), Math.hypot(x1, y0)),
                Math.max(Math.hypot(x0, y1), Math.hypot(x1, y1)));
    }
}
