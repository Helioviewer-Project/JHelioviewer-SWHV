package org.helioviewer.jhv.image;

import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.Region;

record SunCenteredRegion(Region region) {

    static SunCenteredRegion fromImageRegion(Region imageRegion, Vec2 sunShift) {
        return new SunCenteredRegion(new Region(imageRegion.llx - sunShift.x, imageRegion.lly - sunShift.y,
                imageRegion.width, imageRegion.height));
    }
}
