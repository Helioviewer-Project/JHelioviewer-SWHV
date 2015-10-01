package org.helioviewer.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.nio.Buffer;
import java.nio.ShortBuffer;

import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;

/**
 * Representation of image data in single channel format, using 9 to 16 bits per
 * pixel.
 * 
 * <p>
 * Note that this is the only implementation of ImageData, which is supposed to
 * handle a variable number of bits per pixel.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class SingleChannelShortImageData extends AbstractImageData {

    private SingleChannelImageFormat format;

    /**
     * Constructor, given an array as data source.
     * 
     * <p>
     * This constructor receives the raw data as a data source. If the caller
     * handles raw data as well, the use of this constructor is recommended.
     * <p>
     * The pixel data has to be given as a one-dimensional array containing the
     * pixel data line by line. Each array element represents one pixel.
     * 
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     * @param newBitDepth
     *            number of bits per pixel
     * @param newPixelData
     *            pixel data
     */
    public SingleChannelShortImageData(int newWidth, int newHeight, int newBitDepth, Buffer _buffer) {
        super(newWidth, newHeight, 16);
        format = new SingleChannelImageFormat(newBitDepth);
        buffer = _buffer;
    }

    /**
     * Constructor, given an BufferedImage as data source.
     * 
     * <p>
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     * 
     * @param newBitDepth
     *            number of bits per pixel
     * @param newImage
     *            pixel data
     */
    public SingleChannelShortImageData(int newBitDepth, BufferedImage newImage) {
        super(newImage.getWidth(), newImage.getHeight(), 16);
        image = newImage;
        format = new SingleChannelImageFormat(newBitDepth);
        buffer = ShortBuffer.wrap(((DataBufferUShort) newImage.getRaster().getDataBuffer()).getData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort dataBuffer = new DataBufferUShort((short[]) buffer.array(), width * height);

        // create the appropriate bit mask
        int mask = 0xffffffff;
        mask = mask >>> (32 - format.getBitDepth());

        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { mask }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

}
