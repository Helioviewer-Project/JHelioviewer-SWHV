package org.helioviewer.jhv.image;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public record FilterRegion(MetaData metaData, int roiX, int roiY, double factorX, double factorY) {
    Region sunCentered(int width, int height) {
        Region r = metaData.roiToRegion(roiX, roiY, width, height, factorX, factorY);
        Vec2 shift = metaData.getSunShift();
        return new Region(r.llx - shift.x, r.lly - shift.y, r.width, r.height);
    }
}
