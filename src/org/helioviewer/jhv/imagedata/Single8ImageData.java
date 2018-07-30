package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class Single8ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single8;

    public Single8ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 8, 1, _buffer);
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
