package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Vec3;

public abstract class AbstractMetaData implements MetaData {

    int frameNumber = 0;
    Region region;

    int pixelWidth;
    int pixelHeight;
    final float brightnessFactor = 1; // tbd

    Position.L viewpointL = Sun.EpochEarthL;
    Position.Q viewpoint = Sun.EpochEarthQ;
    double innerRadius = 0;
    double outerRadius = Double.MAX_VALUE;

    // Serves only for LASCO cutOff edges
    double cutOffValue = -1;
    Vec3 cutOffDirection;

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
        return pixelWidth;
    }

    @Override
    public int getPixelHeight() {
        return pixelHeight;
    }

    @Override
    public float getBrightnessFactor() {
        return brightnessFactor;
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

}
