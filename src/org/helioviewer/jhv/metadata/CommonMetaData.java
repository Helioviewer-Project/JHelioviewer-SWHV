package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.WcsHeader;

class CommonMetaData implements MetaData {

    private static final double DEFAULT_UNIT_PER_ARCSEC = Sun.Radius / (Math.toDegrees(Math.atan2(Sun.Radius, Sun.MeanEarthDistance)) * 3600);

    protected Region region = Region.DEFAULT;
    protected String displayName = "unknown";
    protected DetectorMask detectorMask = DetectorMask.NONE;

    protected boolean calculateDepth;

    protected double unitPerPixelX = 1;
    protected double unitPerPixelY = 1;
    protected double unitPerArcsec = DEFAULT_UNIT_PER_ARCSEC;
    protected double sunShiftX = 0;
    protected double sunShiftY = 0;
    protected float responseFactor = 1;

    protected Position viewpoint = Sun.StartEarth;
    protected float innerRadius = 0;
    protected float outerRadius = Float.MAX_VALUE;

    protected Vec2 crval = new Vec2(0, 0);
    protected Quat crota = Quat.ZERO;

    protected float sector0 = 0;
    protected float sector1 = 0;

    protected final float[] pv2 = new float[6];
    protected WcsHeader.Projection wcsProjection = WcsHeader.Projection.TAN;
    protected float wcsPlaneUnitsPerRad = (float) Sun.MeanEarthDistance;
    protected WcsHeader wcsHeader;
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
    public Region getPhysicalRegion() {
        return region;
    }

    @Override
    public double getUnitPerPixelY() {
        return unitPerPixelY;
    }

    @Override
    public double getUnitPerArcsec() {
        return unitPerArcsec;
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

    @Nonnull
    @Override
    public WcsHeader getWcsHeader() {
        return wcsHeader;
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

    @Nonnull
    @Override
    public Region roiToSunRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY) {
        Region r = roiToRegion(roiX, roiY, roiWidth, roiHeight, factorX, factorY);
        if (sunShiftX == 0 && sunShiftY == 0)
            return r;
        // Move the origin from the WCS reference point to the Sun center
        return new Region(r.llx - sunShiftX, r.lly + sunShiftY, r.width, r.height);
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
