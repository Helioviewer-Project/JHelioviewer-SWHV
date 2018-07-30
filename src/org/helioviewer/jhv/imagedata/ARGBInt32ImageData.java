package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class ARGBInt32ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.ARGB32;

    public ARGBInt32ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 32, 1, _buffer);
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
