package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.wcs.WcsHeader;

public class PixelBasedMetaData extends BaseMetaData {

    public static final PixelBasedMetaData EMPTY = new PixelBasedMetaData(1, 1, "");

    public PixelBasedMetaData(int _pixelW, int _pixelH, String _displayName) {
        pixelW = _pixelW;
        pixelH = _pixelH;

        double unitPerPixel = Sun.Radius / Math.max(pixelW, pixelH);
        unitPerPixelX = unitPerPixel;
        unitPerPixelY = unitPerPixel;
        region = new Region(-0.5 * pixelW * unitPerPixel, -0.5 * pixelH * unitPerPixel, pixelW * unitPerPixel, pixelH * unitPerPixel);

        displayName = _displayName;
        wcsHeader = new WcsHeader(wcsProjection, pv2, wcsPlaneUnitsPerRad, crval, crota);
    }

}
