package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    public static final String nanValue = String.format("%9s", "--");

    private Position viewpoint;
    private boolean uploaded;

    private final ImageBuffer imageBuffer;
    private final MetaData metaData;
    private final String unit;
    private final float[] physLUT;
    private final Region region;

    public ImageData(@Nonnull ImageBuffer _imageBuffer, @Nonnull MetaData _metaData, @Nonnull Region _region) {
        imageBuffer = _imageBuffer;
        metaData = _metaData;
        unit = metaData.getUnit();
        physLUT = metaData.getPhysicalLUT();
        region = _region;
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

    @Nonnull
    public MetaData getMetaData() {
        return metaData;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

    private float getPixel(double x, double y) {
        double ccr = metaData.getCCROTA();
        double scr = -metaData.getSCROTA();
        double xr = x * ccr - y * scr;
        double yr = x * scr + y * ccr;
        double xf = metaData.xPixelFactor(xr);
        double yf = metaData.yPixelFactor(yr);

        int ix = (int) (xf * (imageBuffer.width - 1) + .5);
        int iy = (int) (yf * (imageBuffer.height - 1) + .5);
        return imageBuffer.getPixel(ix, iy, physLUT);
    }

    @Nonnull
    public String getPixelString(double x, double y) {
        float v = getPixel(x, y);
        String ret;
        if (v == ImageBuffer.BAD_PIXEL)
            ret = nanValue;
        else if (v == (int) v)
            ret = String.format("%9d", (int) v);
        else
            ret = String.format("%9.2f", v);
        return ret + unit;
    }

}
