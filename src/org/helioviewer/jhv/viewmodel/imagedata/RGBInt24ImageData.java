package org.helioviewer.jhv.viewmodel.imagedata;

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

    public RGBInt24ImageData(BufferedImage newImage) {
        super(newImage.getWidth(), newImage.getHeight(), 32, 1);
        image = newImage;
        buffer = IntBuffer.wrap(((DataBufferInt) newImage.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

    @Override
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        newImage.setRGB(0, 0, width, height, (int[]) buffer.array(), 0, width);
        return newImage;
    }

}
