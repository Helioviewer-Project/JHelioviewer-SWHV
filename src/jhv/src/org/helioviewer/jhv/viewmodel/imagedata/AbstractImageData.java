package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;

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

    private static final double BRIGHTNESS_F1 = 0.001;
    private static final double BRIGHTNESS_F2 = 128 + 64 + 32;

    @Override
    public double getAutoBrightness() {
        if (!(buffer instanceof ByteBuffer))
            return 1;

        byte[] ba = ((ByteBuffer) buffer).array();
        int len = ba.length;
        int[] histogram = new int[256];
        for (int i = 0; i < len; i++) {
            histogram[getUnsigned(ba[i])]++;
        }

        long ct = 0;
        int j;
        for (j = 255; j >= 0; j--) {
            ct += histogram[j];
            if (ct > BRIGHTNESS_F1 * len) {
                break;
            }
        }

        double factor = BRIGHTNESS_F2 / j;
        // System.out.println(">> " + factor + " " + j);
        if (j != 0) {
            factor /= metaData.getResponseFactor();
            if (factor > 2)
                factor = 2;
            else if (factor < 0.5)
                factor = 0.5;
            return factor;
        }
        return 1;
    }

    private static int getUnsigned(byte b) {
        return (b + 256) & 0xFF;
    }

}
