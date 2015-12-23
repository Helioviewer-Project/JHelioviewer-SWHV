package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.Buffer;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public abstract class AbstractImageData implements ImageData {

    protected final int width, height;
    protected final int bpp;
    protected Buffer buffer;

    protected BufferedImage image = null;

    private Rectangle roi;
    private Region region;
    private MetaData metaData;
    private Position.Q viewpoint;
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
        roi = new Rectangle(0, 0, newWidth, newHeight);
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public Rectangle getROI() {
        return roi;
    }

    @Override
    public void setROI(final Rectangle r) {
        roi = r;
    }

    @Override
    public int getBitsPerPixel() {
        return bpp;
    }

    @Override
    public Buffer getBuffer() {
        return buffer;
    }

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
    public void setViewpoint(Position.Q p) {
        viewpoint = p;
    }

    @Override
    public Position.Q getViewpoint() {
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
