package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

public class ImageData {

    public static final String nanValue = "----,----      ----";

    private boolean uploaded;

    private final ImageBuffer imageBuffer;

    private final MetaData metaData;
    private final Vec2 crval;
    private final Quat crota;

    private final String unit;
    private final float[] physLUT;
    private final Region region;
    private final Position viewpoint;
    private final boolean hasLUT;

    public ImageData(@Nonnull ImageBuffer _imageBuffer, @Nonnull MetaData _metaData, @Nonnull Region _region, @Nonnull Position _viewpoint) {
        imageBuffer = _imageBuffer;

        metaData = _metaData;
        crval = metaData.getCRVAL();
        crota = metaData.getCROTA();

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

    @Nonnull
    public String getPixelString(float x, float y) {
        Vec3 r = crota.rotateInverseVector(new Vec3(x - crval.x, y - crval.y, 0));
        float xf = (float) metaData.xPixelFactor(r.x);
        float yf = (float) metaData.yPixelFactor(r.y);

        int ix = (int) (xf * (imageBuffer.width - 1) + .5f);
        int iy = (int) (yf * (imageBuffer.height - 1) + .5f);
        float v = imageBuffer.getPixel(ix, iy, physLUT);

        String ret;
        if (v == ImageBuffer.BAD_PIXEL)
            ret = nanValue;
        else if (v == (int) v)
            ret = String.format("%4d,%4d %9d", ix, iy, (int) v);
        else
            ret = String.format("%4d,%4d %9.2f", ix, iy, v);

        if (hasLUT)
            ret += unit;
        return ret;
    }

}
