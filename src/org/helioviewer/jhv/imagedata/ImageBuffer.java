package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

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

    int getPixel(int x, int y) {
        if (x < 0 || x > width - 1 || y < 0 || y > height - 1)
            return ImageData.BAD_PIXEL;

        int idx = x + y * width;
        if (buffer instanceof ByteBuffer)
            return (((ByteBuffer) buffer).get(idx) + 256) & 0xFF;
        if (buffer instanceof ShortBuffer)
            return (((ShortBuffer) buffer).get(idx) + 65536) & 0xFFFF;
        return ImageData.BAD_PIXEL;
    }

}
