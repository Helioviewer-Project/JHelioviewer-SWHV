package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.nio.Buffer;
import java.nio.ShortBuffer;

public class Single16ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.Single16;

    public Single16ImageData(int _width, int _height, double _gamma, Buffer _buffer) {
        super(_width, _height, 16, _gamma);
        buffer = _buffer;
    }

    public Single16ImageData(double _gamma, BufferedImage _image) {
        super(_image.getWidth(), _image.getHeight(), 16, _gamma);
        image = _image;
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
