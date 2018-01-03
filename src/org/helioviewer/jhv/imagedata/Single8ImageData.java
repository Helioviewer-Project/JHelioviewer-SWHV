package org.helioviewer.jhv.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.Buffer;

public class Single8ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single8;

    public Single8ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 8, 1);
        buffer = _buffer;
    }

    public Single8ImageData(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), 8, 1);
        buffer = ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
