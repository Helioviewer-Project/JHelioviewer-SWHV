package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.position.Position;

class BaseMetaData implements MetaData {

    protected int frameNumber = 0;
    protected Region region;
    protected String displayName = "unknown";
    protected String unit = "";
    protected float[] physLUT;

    protected boolean calculateDepth;

    protected int pixelW;
    protected int pixelH;
    protected double unitPerPixelX = 1;
    protected double unitPerPixelY = 1;
    protected double unitPerArcsec = Double.NaN;
    protected double responseFactor = 1;

    protected Position viewpoint = Sun.StartEarth;
    protected double innerRadius = 0;
    protected double outerRadius = Double.MAX_VALUE;

    protected float crota = 0;
    protected float scrota = 0;
    protected float ccrota = 1;

    protected float sector0 = 0;
    protected float sector1 = 0;

    // Serves only for LASCO cutOff edges
    protected float cutOffValue = -1;
    protected float cutOffX = 0;
    protected float cutOffY = 0;

    @Override
    public int getFrameNumber() {
        return frameNumber;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    @Override
    public Region getPhysicalRegion() {
        return region;
    }

    @Override
    public int getPixelWidth() {
        return pixelW;
    }

    @Override
    public int getPixelHeight() {
        return pixelH;
    }

    @Override
    public double getUnitPerArcsec() {
        return unitPerArcsec;
    }

    @Override
    public double getResponseFactor() {
        return responseFactor;
    }

    @Nonnull
    @Override
    public Position getViewpoint() {
        return viewpoint;
    }

    @Override
    public double getInnerRadius() {
        return innerRadius;
    }

    @Override
    public double getOuterRadius() {
        return outerRadius;
    }

    @Override
    public float getCutOffValue() {
        return cutOffValue;
    }

    @Override
    public float getCutOffX() {
        return cutOffX;
    }

    @Override
    public float getCutOffY() {
        return cutOffValue;
    }

    @Nonnull
    @Override
    public Quat getCenterRotation() {
        return viewpoint.toQuat();
    }

    @Override
    public float getCROTA() {
        return crota;
    }

    @Override
    public float getSCROTA() {
        return scrota;
    }

    @Override
    public float getCCROTA() {
        return ccrota;
    }

    @Override
    public float getSector0() {
        return sector0;
    }

    @Override
    public float getSector1() {
        return sector1;
    }

    @Nonnull
    @Override
    public Region roiToRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY) {
        return new Region(roiX * factorX * unitPerPixelX + region.llx, roiY * factorY * unitPerPixelY + region.lly,
                roiWidth * factorX * unitPerPixelX, roiHeight * factorY * unitPerPixelY);
    }

    @Override
    public double xPixelFactor(double xPoint) {
        return (xPoint - region.llx) / unitPerPixelX / pixelW;
    }

    @Override
    public double yPixelFactor(double yPoint) {
        return 1 - (yPoint - region.lly) / unitPerPixelY / pixelH;
    }

    @Nonnull
    @Override
    public String getUnit() {
        return unit;
    }

    @Nullable
    @Override
    public float[] getPhysicalLUT() {
        return physLUT;
    }

    @Override
    public boolean getCalculateDepth() {
        return calculateDepth;
    }

}
