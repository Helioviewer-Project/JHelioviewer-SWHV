package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class ImageBuffer {

    final int width;
    final int height;
    final ImageData.ImageFormat format;
    final Buffer buffer;

    public ImageBuffer(int _width, int _height, ImageData.ImageFormat _format, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
    }

}
