package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.nio.Buffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

/**
 * Abstract ImageData object to provide some common functionalities.
 *
 * The object manages all format-independent informations, such as the image
 * dimensions and the color mask.
 *
 * @author Markus Langenberg
 */
public abstract class AbstractImageData implements ImageData {

    protected final int width, height;
    protected final int bpp;
    protected Buffer buffer;

    protected BufferedImage image = null;

    private int frameNumber;
    private Region region;
    private MetaData metaData;
    private Viewpoint viewpoint;
    private boolean uploaded = false;

    /**
     * Default constructor.
     *
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     */
    protected AbstractImageData(int newWidth, int newHeight, int newBpp) {
        width = newWidth;
        height = newHeight;
        bpp = newBpp;
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

    @Override
    public int getBitsPerPixel() {
        return bpp;
    }

    @Override
    public Buffer getBuffer() {
        return buffer;
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
    public void setFrameNumber(int f) {
        frameNumber = f;
    }

    @Override
    public int getFrameNumber() {
        return frameNumber;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public void setRegion(Region r) {
        region = r;
    }

    @Override
    public void setMetaData(MetaData m) {
        metaData = m;
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public void setViewpoint(Viewpoint v) {
        viewpoint = v;
    }

    @Override
    public Viewpoint getViewpoint() {
        return viewpoint;
    }

    @Override
    public boolean getUploaded() {
        return uploaded;
    }

    @Override
    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

}
