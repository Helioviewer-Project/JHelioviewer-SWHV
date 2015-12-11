package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Vec3;

public abstract class AbstractMetaData implements MetaData {

    protected Region region;

    protected int pixelWidth;
    protected int pixelHeight;

    protected Position.L viewpointL = Sun.EpochEarthL;
    protected Position.Q viewpoint = Sun.EpochEarthQ;
    protected double innerRadius = 0;
    protected double outerRadius = Double.MAX_VALUE;

    // Serves only for LASCO cutOff edges
    protected double cutOffValue = -1;
    protected Vec3 cutOffDirection;

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
