package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;

public class ImageBuffer {

    public enum Format {
        Gray8(1), Gray16(2), ARGB32(4);

        public final int bytes;

        Format(int _bytes) {
            bytes = _bytes;
        }
    }

    public final int width;
    public final int height;
    public final Format format;
    public final Buffer buffer;

    public ImageBuffer(int _width, int _height, Format _format, Buffer _buffer) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
    }

}
