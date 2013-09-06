package org.helioviewer.viewmodel.imagedata;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import org.helioviewer.viewmodel.imageformat.ImageFormat;
import org.helioviewer.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;

/**
 * Representation of image data in single channel format, using 8 bits per
 * pixel.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class SingleChannelByte8ImageData extends AbstractImageData {

    private static final ImageFormat format = new SingleChannelImageFormat(8);
    private Byte8ImageTransport imageTransport;

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
    public SingleChannelByte8ImageData(int newWidth, int newHeight, byte[] newPixelData, ColorMask newColorMask) {
        super(newWidth, newHeight, newColorMask);
        imageTransport = new Byte8ImageTransport(newPixelData);
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
    public SingleChannelByte8ImageData(ImageData base, byte[] newPixelData) {
        super(base);
        imageTransport = new Byte8ImageTransport(newPixelData);
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
    public SingleChannelByte8ImageData(BufferedImage newImage, ColorMask newColorMask) {
        super(newImage.getWidth(), newImage.getHeight(), newColorMask);
        image = newImage;
        imageTransport = new Byte8ImageTransport(((DataBufferByte) newImage.getRaster().getDataBuffer()).getData());
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
    public SingleChannelByte8ImageData(ImageData base, BufferedImage newImage) {
        super(base);
        image = newImage;
        imageTransport = new Byte8ImageTransport(((DataBufferByte) newImage.getRaster().getDataBuffer()).getData());
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
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = new DataBufferByte(imageTransport.getByte8PixelData(), width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }
}
