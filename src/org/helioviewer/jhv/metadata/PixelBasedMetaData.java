package org.helioviewer.jhv.metadata;

import java.net.URI;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;

public class PixelBasedMetaData extends BaseMetaData {

    public PixelBasedMetaData(int _pixelW, int _pixelH, URI uri) {
        pixelW = _pixelW;
        pixelH = _pixelH;

        region = new Region(-0.5, -0.5, 1, 1);
        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;

        if (uri != null) {
            String uriPath = uri.getPath();
            displayName = uriPath.substring(uriPath.lastIndexOf('/') + 1, uriPath.lastIndexOf('.')).intern();
        }
    }

}
