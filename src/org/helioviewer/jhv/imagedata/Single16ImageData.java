package org.helioviewer.jhv.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.nio.Buffer;
import java.nio.ShortBuffer;

public class Single16ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single16;

    public Single16ImageData(int _width, int _height, double _gamma, Buffer _buffer) {
        super(_width, _height, 16, _gamma);
        buffer = _buffer;
    }

    public Single16ImageData(double _gamma, BufferedImage image) {
        super(image.getWidth(), image.getHeight(), 16, _gamma);
        buffer = ShortBuffer.wrap(((DataBufferUShort) image.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
