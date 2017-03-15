package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imageformat.RGB24ImageFormat;

/**
 * Representation of image data in RGB24 format.
 *
 * The image data contains three channels (red, green, blue), each channel has
 * eight bits per pixel.
 */
public class RGBInt24ImageData extends ImageData {

    private static final ImageFormat format = new RGB24ImageFormat();

    /**
     * Constructor, given an array as data source.
     *
     * This constructor receives the raw data as a data source. If the caller
     * handles raw data as well, the use of this constructor is recommended.
     * The pixel data has to be given as a one-dimensional array containing the
     * pixel data line by line. Each array element represents one pixel.
     *
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     * @param newPixelData
     *            pixel data
     */
    public RGBInt24ImageData(int newWidth, int newHeight, Buffer _buffer) {
        super(newWidth, newHeight, 32);
        buffer = _buffer;
    }

    /**
     * Constructor, given an BufferedImage as data source.
     *
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     *
     * @param newImage
     *            pixel data
     */
    public RGBInt24ImageData(BufferedImage newImage) {
        super(newImage.getWidth(), newImage.getHeight(), 32);
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
