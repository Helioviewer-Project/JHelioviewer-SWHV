package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.position.Position;

abstract class AbstractMetaData implements MetaData {

    int frameNumber = 0;
    Region region;

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
    double sinCrota = 0;
    double cosCrota = 1;

    // Serves only for LASCO cutOff edges
    double cutOffValue = -1;
    Vec3 cutOffDirection = Vec3.ZERO;

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
    public double getInnerCutOffRadius() {
        return innerRadius;
    }

    @Override
    public double getOuterCutOffRadius() {
        return outerRadius;
    }

    @Override
    public double getCutOffValue() {
        return cutOffValue;
    }

    @Nonnull
    @Override
    public Vec3 getCutOffDirection() {
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
    public double getSinCROTA() {
        return sinCrota;
    }
    @Override
    public double getCosCROTA() {
        return cosCrota;
    }

    @Override
    public double xPixelFactor(double xPoint) {
        return (xPoint - region.llx) / unitPerPixelX / pixelW;
    }

    @Override
    public double yPixelFactor(double yPoint) {
        return 1 - (yPoint - region.lly) / unitPerPixelY / pixelH;
    }

}
