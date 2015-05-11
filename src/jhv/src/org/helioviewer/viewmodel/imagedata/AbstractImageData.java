package org.helioviewer.viewmodel.imagedata;

import java.awt.image.BufferedImage;

import org.helioviewer.base.Region;
import org.helioviewer.viewmodel.metadata.MetaData;

/**
 * Abstract ImageData object to provide some common functionalities.
 *
 * The object manages all format-independent informations, such as the image
 * dimensions and the color mask.
 *
 * @author Markus Langenberg
 */
public abstract class AbstractImageData implements ImageData {

    protected int width, height;
    protected BufferedImage image = null;

    private Region region;
    private MetaData metadata;

    private int frameNumber;

    /**
     * Default constructor.
     *
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     */
    protected AbstractImageData(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
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
    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    @Override
    public int getFrameNumber() {
        return this.frameNumber;
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
    public MetaData getMETADATA() {
        return this.metadata;
    }

    @Override
    public void setMETADATA(MetaData m) {
        this.metadata = m;
    }

}
