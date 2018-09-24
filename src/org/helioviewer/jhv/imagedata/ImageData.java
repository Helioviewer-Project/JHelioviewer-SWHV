package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.position.Position;

public class ImageData {

    public static final String nanValue = String.format("%7s", "--");

    private Position viewpoint;
    private Region region;
    private MetaData metaData;
    private boolean uploaded = false;

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
        int v = getPixel(x, y);
        if (v == ImageBuffer.BAD_PIXEL)
            return nanValue;
        return String.format("%7d", v);
    }

}
