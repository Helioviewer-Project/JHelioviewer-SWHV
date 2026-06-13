package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.wcs.WcsHeader;

public class BasicMetaData extends CommonMetaData {

    public static final BasicMetaData EMPTY = new BasicMetaData(1, 1, "");

    public BasicMetaData(int pixelW, int pixelH, String _displayName) {
        double unitPerPixel = Sun.Radius / Math.max(pixelW, pixelH);
        unitPerPixelX = unitPerPixel;
        unitPerPixelY = unitPerPixel;
        region = new Region(-0.5 * pixelW * unitPerPixel, -0.5 * pixelH * unitPerPixel, pixelW * unitPerPixel, pixelH * unitPerPixel);

        displayName = _displayName;
        wcsHeader = new WcsHeader(wcsProjection, pv2, wcsPlaneUnitsPerRad, crval, crota);
    }

}
