package org.helioviewer.jhv.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.IntBuffer;

public class RGBInt24ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.RGB24;

    public RGBInt24ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 32, 1);
        buffer = _buffer;
    }

    public RGBInt24ImageData(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), 32, 1);
        buffer = IntBuffer.wrap(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

}
