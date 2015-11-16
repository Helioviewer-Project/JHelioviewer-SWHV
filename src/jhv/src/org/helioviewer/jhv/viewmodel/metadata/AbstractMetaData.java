package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

public abstract class AbstractMetaData implements MetaData {

    protected Region region;

    protected int pixelWidth;
    protected int pixelHeight;

    protected JHVDate dateObs = TimeUtils.epoch;
    protected Quat rotationObs = Quat.ZERO;
    protected double distanceObs = Sun.MeanEarthDistance;
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
    public JHVDate getDateObs() {
        return dateObs;
    }

    @Override
    public Quat getRotationObs() {
        return rotationObs;
    }

    @Override
    public double getDistanceObs() {
        return distanceObs;
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
