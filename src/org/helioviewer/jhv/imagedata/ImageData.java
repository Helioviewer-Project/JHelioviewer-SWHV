package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public abstract class ImageData {

    public enum ImageFormat {
        Single8, Single16, ARGB32
    }

    private final int width;
    private final int height;
    private final int bpp;
    private final double gamma;
    private final Buffer buffer;

    private Region region;
    private MetaData metaData;
    private Position viewpoint;
    private boolean uploaded = false;

    ImageData(int _width, int _height, int _bpp, double _gamma, Buffer _buffer) {
        width = _width;
        height = _height;
        bpp = _bpp;
        gamma = _gamma;
        buffer = _buffer;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public double getGamma() {
        return gamma;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public abstract ImageFormat getImageFormat();

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region r) {
        region = r;
    }

    public void setMetaData(MetaData m) {
        metaData = m;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setViewpoint(Position p) {
        viewpoint = p;
    }

    public Position getViewpoint() {
        return viewpoint;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

    private static final double BRIGHTNESS_F1 = 0.001;
    private static final double BRIGHTNESS_F2 = 128 + 64 + 32;

    public double getAutoBrightness() {
        if (!(buffer instanceof ByteBuffer))
            return 1;

        int len = buffer.capacity();
        int[] histogram = new int[256];
        ByteBuffer byteBuffer = (ByteBuffer) buffer;
        for (int i = 0; i < len; i++) {
            histogram[getUnsigned(byteBuffer.get(i))]++;
        }

        long ct = 0;
        int j;
        for (j = 255; j >= 0; j--) {
            ct += histogram[j];
            if (ct > BRIGHTNESS_F1 * len) {
                break;
            }
        }
        if (j == 0)
            return 1;

        double factor = BRIGHTNESS_F2 / j;
        // System.out.println(">> " + factor + " " + j);
        factor /= metaData.getResponseFactor();
        return MathUtils.clip(factor, 0.5, 2);
    }

    private static int getUnsigned(byte b) {
        return (b + 256) & 0xFF;
    }

}
