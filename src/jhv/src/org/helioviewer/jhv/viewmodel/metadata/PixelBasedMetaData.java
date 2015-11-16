package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;

public class PixelBasedMetaData extends AbstractMetaData {

    public PixelBasedMetaData(int newWidth, int newHeight) {
        region = new Region(-1.5, -1.5, 3., 3.);

        pixelWidth = newWidth;
        pixelHeight = newHeight;
    }

}
