package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class ImageDataBuffer {

    final int width;
    final int height;
    final ImageData.ImageFormat format;
    final Buffer buffer;
    public final int bufferID;

    public ImageDataBuffer(int _width, int _height, ImageData.ImageFormat _format, Buffer _buffer, int _bufferID) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
        bufferID = _bufferID;
    }

}
