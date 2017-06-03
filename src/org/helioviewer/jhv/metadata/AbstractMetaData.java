package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

public abstract class AbstractMetaData implements MetaData {

    int frameNumber = 0;
    Region region;

    int pixelW;
    int pixelH;
    double responseFactor = 1;

    Position.L viewpointL = Sun.EpochEarthL;
    Position.Q viewpoint = Sun.EpochEarthQ;
    double innerRadius = 0;
    double outerRadius = Double.MAX_VALUE;
    double crota;

    // Serves only for LASCO cutOff edges
    double cutOffValue = -1;
    Vec3 cutOffDirection = Vec3.ZERO;

    @Override
    public int getFrameNumber() {
        return frameNumber;
    }

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
    public double getResponseFactor() {
        return responseFactor;
    }

    @Override
    public Position.Q getViewpoint() {
        return viewpoint;
    }

    @Override
    public Position.L getViewpointL() {
        return viewpointL;
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

    @Override
    public Vec3 getCutOffDirection() {
        return cutOffDirection;
    }

    @Override
    public Quat getCenterRotation() {
        return viewpoint.orientation;
    }

    @Override
    public double getCROTA() {
        return crota;
    }

}
