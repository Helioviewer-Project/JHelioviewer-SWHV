package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.SubImage;

public class PixelBasedMetaData extends AbstractMetaData {

    public PixelBasedMetaData(int newW, int newH, int frame) {
        frameNumber = frame;

        region = new Region(-0.5, -0.5, 1, 1);
        pixelW = newW;
        pixelH = newH;

        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;
    }

    @Nonnull
    @Override
    public Region roiToRegion(@Nonnull SubImage roi, double factorX, double factorY) {
        return new Region(roi.x * factorX * unitPerPixelX - 0.5, roi.y * factorY * unitPerPixelY - 0.5,
                roi.width * factorX * unitPerPixelX, roi.height * factorY * unitPerPixelY);
    }

}
