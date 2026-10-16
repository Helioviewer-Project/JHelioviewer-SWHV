package org.helioviewer.jhv.metadata;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.wcs.ImageGeometry;

class CommonMetaData implements MetaData {

    private static final ImageGeometry DEFAULT_GEOMETRY = new ImageGeometry(1, 1, 1);

    protected ImageGeometry geometry = DEFAULT_GEOMETRY;
    protected String displayName = "unknown";
    protected URI sourceUri = MetaData.UNKNOWN_SOURCE_URI;
    protected DetectorMask detectorMask = DetectorMask.NONE;

    protected boolean calculateDepth;

    protected float responseFactor = 1;

    protected Position viewpoint = Sun.StartEarth;
    protected float innerRadius = 0;
    protected float outerRadius = Float.MAX_VALUE;

    protected float sector0 = 0;
    protected float sector1 = 0;

    // Serves only for LASCO cutOff edges
    protected float cutOffValue = -1;
    protected float cutOffX = 0;
    protected float cutOffY = 0;

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    @Override
    public URI getSourceUri() {
        return sourceUri;
    }

    @Nonnull
    @Override
    public ImageGeometry getGeometry() {
        return geometry;
    }

    @Override
    public float getResponseFactor() {
        return responseFactor;
    }

    @Nonnull
    @Override
    public Position getViewpoint() {
        return viewpoint;
    }

    @Override
    public float getInnerRadius() {
        return innerRadius;
    }

    @Override
    public float getOuterRadius() {
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
        return cutOffY;
    }

    @Override
    public float getSector0() {
        return sector0;
    }

    @Override
    public float getSector1() {
        return sector1;
    }

    @Override
    public boolean getCalculateDepth() {
        return calculateDepth;
    }

    @Nonnull
    @Override
    public DetectorMask getDetectorMask() {
        return detectorMask;
    }

}
