package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

import org.helioviewer.jhv.opengl.GLTexture;

public class ImageBuffer {

    final int width;
    final int height;
    final ImageData.ImageFormat format;
    final Buffer buffer;
    public final int texID;

    public ImageBuffer(int _width, int _height, ImageData.ImageFormat _format, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;

        texID = GLTexture.generate(width, height, format, _buffer);
        buffer = null; // fix callisto _buffer;
    }

    public void delete() {
        GLTexture.delete(texID);
    }

}
