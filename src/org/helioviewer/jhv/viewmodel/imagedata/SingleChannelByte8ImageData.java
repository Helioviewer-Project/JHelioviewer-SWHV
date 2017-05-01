package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.Buffer;

import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imageformat.SingleChannelImageFormat;

/**
 * Representation of image data in single channel format, using 8 bits per
 * pixel.
 */
public class SingleChannelByte8ImageData extends ImageData {

    private static final ImageFormat format = new SingleChannelImageFormat(8);

    public SingleChannelByte8ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 8, 1);
        buffer = _buffer;
    }

    public SingleChannelByte8ImageData(BufferedImage newImage) {
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
