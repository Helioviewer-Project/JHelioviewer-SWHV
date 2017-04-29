package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;

public class PixelBasedMetaData extends AbstractMetaData {

    private final double unitPerPixelX;
    private final double unitPerPixelY;

    public PixelBasedMetaData(int newW, int newH, int frame) {
        frameNumber = frame;

        region = new Region(-0.5, -0.5, 1, 1);
        pixelW = newW;
        pixelH = newH;

        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;
    }

    @Override
    public Region roiToRegion(SubImage roi, double factorX, double factorY) {
        return new Region(roi.x * factorX * unitPerPixelX - 0.5, roi.y * factorY * unitPerPixelY - 0.5,
                          roi.width * factorX * unitPerPixelX, roi.height * factorY * unitPerPixelY);
    }

}
