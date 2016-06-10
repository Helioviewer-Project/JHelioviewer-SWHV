package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;

public class PixelBasedMetaData extends AbstractMetaData {

    double unitPerPixel;

    public PixelBasedMetaData(int newWidth, int newHeight, int frame) {
        frameNumber = frame;

        region = new Region(-1, -1, 2, 2);
        pixelWidth = newWidth;
        pixelHeight = newHeight;
        unitPerPixel = 1. / Math.max(pixelWidth, pixelHeight);
    }

    @Override
    public Region roiToRegion(SubImage roi, double factorX, double factorY) {
        return new Region(roi.x * factorX * unitPerPixel - 0.5, roi.y * factorY * unitPerPixel - 0.5,
                          roi.width * factorX * unitPerPixel, roi.height * factorY * unitPerPixel);
    }

}
