package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.nio.Buffer;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public abstract class AbstractImageData implements ImageData {

    final int width;
    final int height;
    private final int bpp;
    Buffer buffer;

    BufferedImage image = null;

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
    AbstractImageData(int newWidth, int newHeight, int newBpp) {
        width = newWidth;
        height = newHeight;
        bpp = newBpp;
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
