package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class Single16ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single16;

    public Single16ImageData(int _width, int _height, double _gamma, Buffer _buffer) {
        super(_width, _height, 16, _gamma, _buffer);
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
