package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    public static final String nanValue = String.format("%9s", "--");
    public static final int BAD_PIXEL = Integer.MIN_VALUE;

    private Position viewpoint;
    private Region region;
    private MetaData metaData;
    private boolean uploaded;
    private float[] physLUT;
    private String unit = "";

    private final ImageBuffer imageBuffer;

    public ImageData(@Nonnull ImageBuffer _imageBuffer) {
        imageBuffer = _imageBuffer;
    }

    @Nonnull
    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    @Nonnull
    public Position getViewpoint() {
        return viewpoint;
    }

    public void setViewpoint(@Nonnull Position _viewpoint) {
        viewpoint = _viewpoint;
    }

    @Nonnull
    public Region getRegion() {
        return region;
    }

    public void setRegion(@Nonnull Region _region) {
        region = _region;
    }

    @Nonnull
    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(@Nonnull MetaData _metaData) {
        metaData = _metaData;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

    public void setPhysical(@Nonnull float[] _physLUT, @Nonnull String _unit) {
        physLUT = _physLUT;
        unit = _unit;
    }

    private int getPixel(double x, double y) {
        double ccr = metaData.getCCROTA();
        double scr = -metaData.getSCROTA();
        double xr = x * ccr - y * scr;
        double yr = x * scr + y * ccr;
        double xf = metaData.xPixelFactor(xr);
        double yf = metaData.yPixelFactor(yr);

        int ix = (int) (xf * (imageBuffer.width - 1) + .5);
        int iy = (int) (yf * (imageBuffer.height - 1) + .5);
        return imageBuffer.getPixel(ix, iy);
    }

    @Nonnull
    public String getPixelString(double x, double y) {
        float v = getPixel(x, y);
        if (physLUT != null && v != BAD_PIXEL) {
            v = physLUT[(int) v];
        }

        String ret;
        if (v == BAD_PIXEL)
            ret = nanValue;
        else if (v == (int) v)
            ret = String.format("%9d", (int) v);
        else
            ret = String.format("%9.2f", v);
        return ret + unit;
    }

}
