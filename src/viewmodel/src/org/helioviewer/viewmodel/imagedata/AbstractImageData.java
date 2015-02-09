package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

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
    private Region region;
    private SubImage subImage;
    private int frameNumber;
    private double zoomPercent;
    private boolean fullyLoaded;

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
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColorMask getColorMask() {
        return colorMask;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    @Override
    public long getDateMillis() {
        return dateMillis;
    }

    @Override
    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    @Override
    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    @Override
    public int getFrameNumber() {
        return this.frameNumber;
    }

    @Override
    public void setZoomPercent(double percent) {
        this.zoomPercent = percent;
    }

    @Override
    public double getZoomPercent() {
        return this.zoomPercent;
    }

    @Override
    public void setSubImage(SubImage subImage) {
        this.subImage = subImage;
    }

    @Override
    public SubImage getSubImage() {
        return this.subImage;
    }

    @Override
    public Region getRegion() {
        return this.region;
    }

    @Override
    public void setRegion(Region r) {
        this.region = r;
    }

    @Override
    public void setFullyLoaded(boolean fullyLoaded) {
        this.fullyLoaded = fullyLoaded;
    }

    @Override
    public boolean getFullyLoaded() {
        return fullyLoaded;
    }
}
