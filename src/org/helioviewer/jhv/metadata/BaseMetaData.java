package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.position.Position;

class BaseMetaData implements MetaData {

    int frameNumber = 0;
    Region region;
    String unit = "";
    float[] physLUT;

    int pixelW;
    int pixelH;
    double unitPerPixelX = 1;
    double unitPerPixelY = 1;
    double unitPerArcsec = Double.NaN;
    double responseFactor = 1;

    Position viewpoint = Sun.StartEarth;
    double innerRadius = 0;
    double outerRadius = Double.MAX_VALUE;

    double crota = 0;
    double scrota = 0;
    double ccrota = 1;

    double sector0 = 0;
    double sector1 = 0;

    // Serves only for LASCO cutOff edges
    double cutOffValue = -1;
    Vec2 cutOffDirection = Vec2.ZERO;

    @Override
    public int getFrameNumber() {
        return frameNumber;
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
    public double getCutOffValue() {
        return cutOffValue;
    }

    @Nonnull
    @Override
    public Vec2 getCutOffDirection() {
        return cutOffDirection;
    }

    @Nonnull
    @Override
    public Quat getCenterRotation() {
        return viewpoint.toQuat();
    }

    @Override
    public double getCROTA() {
        return crota;
    }

    @Override
    public double getSCROTA() {
        return scrota;
    }

    @Override
    public double getCCROTA() {
        return ccrota;
    }

    @Override
    public double getSector0() {
        return sector0;
    }

    @Override
    public double getSector1() {
        return sector1;
    }

    @Nonnull
    @Override
    public Region roiToRegion(@Nonnull SubImage roi, double factorX, double factorY) {
        return new Region(roi.x * factorX * unitPerPixelX + region.llx, roi.y * factorY * unitPerPixelY + region.lly,
                roi.width * factorX * unitPerPixelX, roi.height * factorY * unitPerPixelY);
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

}
