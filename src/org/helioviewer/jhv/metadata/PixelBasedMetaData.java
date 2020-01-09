package org.helioviewer.jhv.metadata;

import java.net.URI;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;

public class PixelBasedMetaData extends BaseMetaData {

    public PixelBasedMetaData(int _pixelW, int _pixelH, int _frameNumber, URI uri) {
        pixelW = _pixelW;
        pixelH = _pixelH;
        frameNumber = _frameNumber;

        region = new Region(-0.5, -0.5, 1, 1);
        unitPerPixelX = Sun.Radius / pixelW;
        unitPerPixelY = Sun.Radius / pixelH;

        if (uri != null) {
            String path = uri.getPath();
            displayName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')).intern();
        }
    }

}
