package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;

public class PixelBasedMetaData extends BaseMetaData {

    public PixelBasedMetaData(int _pixelW, int _pixelH, String _displayName) {
        pixelW = _pixelW;
        pixelH = _pixelH;

        region = defaultRegion;
        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;

        displayName = _displayName;
    }

    public static PixelBasedMetaData EMPTY = new PixelBasedMetaData(1, 1, "");

}
