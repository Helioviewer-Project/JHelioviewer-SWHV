package org.helioviewer.jhv.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.Buffer;

public class Single8ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single8;

    public Single8ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 8, 1);
        buffer = _buffer;
    }

    public Single8ImageData(BufferedImage newImage) {
        super(newImage.getWidth(), newImage.getHeight(), 8, 1);
        image = newImage;
        buffer = ByteBuffer.wrap(((DataBufferByte) newImage.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

    @Override
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte((byte[]) buffer.array(), width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

}
