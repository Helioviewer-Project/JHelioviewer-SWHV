package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

/**
 * Abstract ImageData object to provide some common functionalities.
 * 
 * The object manages all format-independent informations, such as the image
 * dimensions and the color mask.
 * 
 * @author Markus Langenberg
 */
public abstract class AbstractImageData implements JavaBufferedImageData {

    protected int width, height;
    protected BufferedImage image = null;
    protected ColorMask colorMask;
    private long dateMillis;

    /**
     * Default constructor.
     * 
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     * @param newColorMask
     *            color mask of the image
     */
    protected AbstractImageData(int newWidth, int newHeight, ColorMask newColorMask) {
        width = newWidth;
        height = newHeight;
        colorMask = newColorMask;
    }

    /**
     * Copy constructor.
     * 
     * @param copyFrom
     *            object to copy
     */
    protected AbstractImageData(ImageData copyFrom) {
        AbstractImageData base = (AbstractImageData) copyFrom;

        width = base.width;
        height = base.height;
        colorMask = base.colorMask;
    }

    /**
     * {@inheritDoc}
     */
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    public ColorMask getColorMask() {
        return colorMask;
    }

    /**
     * {@inheritDoc}
     */
    public BufferedImage getBufferedImage() {
        if (image == null) {
            image = createBufferedImageFromImageTransport();
        }
        return image;
    }

    /**
     * Internal function to create a BufferedImage from the image transport
     * object.
     * 
     * This function will be called from {@link #getBufferedImage()} when
     * necessary.
     * 
     * @return the created BufferedImage
     */
    protected abstract BufferedImage createBufferedImageFromImageTransport();

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }
}
