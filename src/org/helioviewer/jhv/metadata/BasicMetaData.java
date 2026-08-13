package org.helioviewer.jhv.metadata;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.wcs.WcsHeader;

public class BasicMetaData extends CommonMetaData {

    public static final BasicMetaData EMPTY = new BasicMetaData(1, 1, "");

    public BasicMetaData(int pixelW, int pixelH, String _displayName) {
        this(pixelW, pixelH, _displayName, MetaData.UNKNOWN_SOURCE_URI);
    }

    public BasicMetaData(int pixelW, int pixelH, String _displayName, @Nonnull URI _sourceUri) {
        double unitPerPixel = Sun.Radius / Math.max(pixelW, pixelH);
        unitPerPixelX = unitPerPixel;
        unitPerPixelY = unitPerPixel;
        region = new Region(-0.5 * pixelW * unitPerPixel, -0.5 * pixelH * unitPerPixel, pixelW * unitPerPixel, pixelH * unitPerPixel);

        displayName = _displayName;
        sourceUri = _sourceUri;
        wcsHeader = new WcsHeader(wcsProjection, pv2, wcsPlaneUnitsPerRad, crval, imageToPlane);
    }

}
