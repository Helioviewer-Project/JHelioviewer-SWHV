package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.base.time.ImmutableDateTime;
import org.helioviewer.jhv.base.time.TimeUtils;

public abstract class AbstractMetaData implements MetaData {

    private Vec2d lowerLeftCorner;
    private Vec2d sizeVector;

    protected int pixelWidth;
    protected int pixelHeight;

    protected ImmutableDateTime dateObs = TimeUtils.epoch;
    protected Quatd rotationObs = Quatd.ZERO;
    protected double distanceObs = Sun.MeanEarthDistance;
    protected double innerRadius = 0.;
    protected double outerRadius = 40.;
    protected double unitPerPixel = 1.;

    //Serves only for LASCO cutOff edges
    protected float cutOffValue = -1;
    protected Vec3d cutOffDirection;

    public AbstractMetaData() {
        lowerLeftCorner = null;
        sizeVector = null;
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCornerX
     *            Physical lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Physical lower left y-coordinate of the corresponding image
     * @param newWidth
     *            Physical width of the corresponding image
     * @param newHeight
     *            Physical height of the corresponding image
     */
    public AbstractMetaData(double newLowerLeftCornerX, double newLowerLeftCornerY, double newWidth, double newHeight) {
        lowerLeftCorner = new Vec2d(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = new Vec2d(newWidth, newHeight);
    }

    /**
     * Sets the physical size of the corresponding image.
     *
     * @param newImageSize
     *            Physical size of the corresponding image
     */
    protected void setPhysicalSize(Vec2d newImageSize) {
        sizeVector = newImageSize;
    }

    /**
     * Sets the physical lower left corner the corresponding image.
     *
     * @param newlLowerLeftCorner
     *            Physical lower left corner the corresponding image
     */
    protected void setPhysicalLowerLeftCorner(Vec2d newlLowerLeftCorner) {
        lowerLeftCorner = newlLowerLeftCorner;
    }

    @Override
    public Vec2d getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    @Override
    public Vec2d getPhysicalUpperLeft() {
        return new Vec2d(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    @Override
    public Vec2d getPhysicalSize() {
        return sizeVector;
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
    public ImmutableDateTime getDateObs() {
        return dateObs;
    }

    @Override
    public Quatd getRotationObs() {
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
    public float getCutOffValue() {
        return cutOffValue;
    }

    @Override
    public Vec3d getCutOffDirection() {
        return cutOffDirection;
    }

}
