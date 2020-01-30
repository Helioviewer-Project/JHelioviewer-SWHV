package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;

public class ImageData {

    public static final String nanValue = String.format("%9s", "--");

    private boolean uploaded;

    private final ImageBuffer imageBuffer;
    private final MetaData metaData;
    private final String unit;
    private final float[] physLUT;
    private final Region region;
    private final Position viewpoint;
    private final boolean hasLUT;

    public ImageData(@Nonnull ImageBuffer _imageBuffer, @Nonnull MetaData _metaData, @Nonnull Region _region, @Nonnull Position _viewpoint) {
        imageBuffer = _imageBuffer;
        metaData = _metaData;
        unit = metaData.getUnit();
        physLUT = metaData.getPhysicalLUT();
        region = _region;
        viewpoint = _viewpoint;
        hasLUT = physLUT != null || imageBuffer.hasLUT();
    }

    @Nonnull
    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    @Nonnull
    public MetaData getMetaData() {
        return metaData;
    }

    @Nonnull
    public Region getRegion() {
        return region;
    }

    @Nonnull
    public Position getViewpoint() {
        return viewpoint;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

    private float getPixel(float x, float y) {
        float ccr = metaData.getCCROTA();
        float scr = -metaData.getSCROTA();
        float xr = x * ccr - y * scr;
        float yr = x * scr + y * ccr;
        float xf = (float) metaData.xPixelFactor(xr);
        float yf = (float) metaData.yPixelFactor(yr);

        int ix = (int) (xf * (imageBuffer.width - 1) + .5f);
        int iy = (int) (yf * (imageBuffer.height - 1) + .5f);
        return imageBuffer.getPixel(ix, iy, physLUT);
    }

    @Nonnull
    public String getPixelString(float x, float y) {
        float v = getPixel(x, y);
        String ret;
        if (v == ImageBuffer.BAD_PIXEL)
            ret = nanValue;
        else if (v == (int) v)
            ret = String.format("%9d", (int) v);
        else
            ret = String.format("%9.2f", v);

        if (hasLUT)
            ret += unit;
        return ret;
    }

}
