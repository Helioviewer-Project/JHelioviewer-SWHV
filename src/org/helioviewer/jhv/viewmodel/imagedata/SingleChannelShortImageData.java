package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.nio.Buffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imageformat.SingleChannelImageFormat;

/**
 * Representation of image data in single channel format, using 9 to 16 bits per
 * pixel.
 *
 * Note that this is the only implementation of ImageData, which is supposed to
 * handle a variable number of bits per pixel.
 */
public class SingleChannelShortImageData extends ImageData {

    private final SingleChannelImageFormat format;

    public SingleChannelShortImageData(int _width, int _height, int _bpp, double _gamma, Buffer _buffer) {
        super(_width, _height, 16, _gamma);
        format = new SingleChannelImageFormat(_bpp);
        buffer = _buffer;
    }

    public SingleChannelShortImageData(int _bpp, double _gamma, BufferedImage _image) {
        super(_image.getWidth(), _image.getHeight(), 16, _gamma);
        image = _image;
        format = new SingleChannelImageFormat(_bpp);
        buffer = ShortBuffer.wrap(((DataBufferUShort) _image.getRaster().getDataBuffer()).getData());
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

    @Override
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort dataBuffer = new DataBufferUShort((short[]) buffer.array(), width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xffff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

}
