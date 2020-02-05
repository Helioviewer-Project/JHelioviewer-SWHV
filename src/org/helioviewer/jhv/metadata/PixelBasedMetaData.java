package org.helioviewer.jhv.metadata;

import java.net.URI;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.Strings;

public class PixelBasedMetaData extends BaseMetaData {

    public PixelBasedMetaData(int _pixelW, int _pixelH, int _frameNumber, URI uri) {
        pixelW = _pixelW;
        pixelH = _pixelH;
        frameNumber = _frameNumber;

        region = new Region(-0.5, -0.5, 1, 1);
        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;

        if (uri != null) {
            String uriPath = uri.getPath();
            displayName = Strings.intern(uriPath.substring(uriPath.lastIndexOf('/') + 1, uriPath.lastIndexOf('.')));
        }
    }

}
