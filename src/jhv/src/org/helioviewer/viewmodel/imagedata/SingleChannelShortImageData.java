package org.helioviewer.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Short16ImageTransport;

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
    private Short16ImageTransport imageTransport;

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
     * @param newColorMask
     *            color mask of the image
     */
    public SingleChannelShortImageData(int newWidth, int newHeight, int newBitDepth, short[] newPixelData, ColorMask newColorMask) {
        super(newWidth, newHeight, newColorMask);
        imageTransport = new Short16ImageTransport(newPixelData);
        format = new SingleChannelImageFormat(newBitDepth);
    }

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
     * @param base
     *            original ImageData-object
     * @param newPixelData
     *            pixel data
     */
    public SingleChannelShortImageData(ImageData base, short[] newPixelData) {
        super(base);
        imageTransport = new Short16ImageTransport(newPixelData);
        format = (SingleChannelImageFormat) base.getImageFormat();
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
     * @param newColorMask
     *            color mask of the image
     */
    public SingleChannelShortImageData(int newBitDepth, BufferedImage newImage, ColorMask newColorMask) {
        super(newImage.getWidth(), newImage.getHeight(), newColorMask);
        image = newImage;
        imageTransport = new Short16ImageTransport(((DataBufferUShort) newImage.getRaster().getDataBuffer()).getData());
        format = new SingleChannelImageFormat(newBitDepth);
    }

    /**
     * Constructor, given an BufferedImage as data source.
     * 
     * <p>
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     * 
     * @param base
     *            original ImageData-object
     * @param newImage
     *            pixel data
     */
    public SingleChannelShortImageData(ImageData base, BufferedImage newImage) {
        super(base);
        image = newImage;
        imageTransport = new Short16ImageTransport(((DataBufferUShort) newImage.getRaster().getDataBuffer()).getData());
        format = (SingleChannelImageFormat) base.getImageFormat();
    }

    /**
     * {@inheritDoc}
     */
    public ImageFormat getImageFormat() {
        return format;
    }

    /**
     * {@inheritDoc}
     */
    public ImageTransport getImageTransport() {
        return imageTransport;
    }

    /**
     * {@inheritDoc}
     */
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort dataBuffer = new DataBufferUShort(imageTransport.getShort16PixelData(), width * height);

        // create the appropriate bit mask
        int mask = 0xffffffff;
        mask = mask >>> (32 - format.getBitDepth());

        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { mask }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }
}
