package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.RGB24ImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Int32ImageTransport;

/**
 * Representation of image data in RGB24 format.
 * 
 * <p>
 * The image data contains three channels (red, green, blue), each channel has
 * eight bits per pixel.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class RGBInt24ImageData extends AbstractImageData {

    private static final ImageFormat format = new RGB24ImageFormat();
    private Int32ImageTransport imageTransport;

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
     * @param newPixelData
     *            pixel data
     * @param newColorMask
     *            color mask of the image
     */
    public RGBInt24ImageData(int newWidth, int newHeight, int[] newPixelData, ColorMask newColorMask) {
        super(newWidth, newHeight, newColorMask);
        imageTransport = new Int32ImageTransport(newPixelData);
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
    public RGBInt24ImageData(ImageData base, int[] newPixelData) {
        super(base);
        imageTransport = new Int32ImageTransport(newPixelData);
    }

    /**
     * Constructor, given an BufferedImage as data source.
     * 
     * <p>
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     * 
     * @param newImage
     *            pixel data
     * @param newColorMask
     *            color mask of the image
     */
    public RGBInt24ImageData(BufferedImage newImage, ColorMask newColorMask) {
        super(newImage.getWidth(), newImage.getHeight(), newColorMask);
        image = newImage;
        imageTransport = new Int32ImageTransport(((DataBufferInt) newImage.getRaster().getDataBuffer()).getData());
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
    public RGBInt24ImageData(ImageData base, BufferedImage newImage) {
        super(base);
        image = newImage;
        imageTransport = new Int32ImageTransport(((DataBufferInt) newImage.getRaster().getDataBuffer()).getData());
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
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        newImage.setRGB(0, 0, width, height, imageTransport.getInt32PixelData(), 0, width);
        return newImage;
    }
}
