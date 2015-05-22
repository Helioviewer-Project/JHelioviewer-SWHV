package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;

public abstract class AbstractMetaData implements MetaData {

    private GL3DVec2d lowerLeftCorner;
    private GL3DVec2d sizeVector;

    protected int pixelWidth;
    protected int pixelHeight;

    protected ImmutableDateTime dateObs = TimeUtils.epoch;
    protected GL3DQuatd rotationObs = GL3DQuatd.ZERO;
    protected double distanceObs = Sun.MeanEarthDistance / Sun.RadiusMeter;
    protected double innerRadius = 0.;
    protected double outerRadius = 40.;
    protected double unitPerPixel = 1.;

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
        lowerLeftCorner = new GL3DVec2d(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = new GL3DVec2d(newWidth, newHeight);
    }

    /**
     * Sets the physical size of the corresponding image.
     *
     * @param newImageSize
     *            Physical size of the corresponding image
     */
    protected void setPhysicalSize(GL3DVec2d newImageSize) {
        sizeVector = newImageSize;
    }

    /**
     * Sets the physical lower left corner the corresponding image.
     *
     * @param newlLowerLeftCorner
     *            Physical lower left corner the corresponding image
     */
    protected void setPhysicalLowerLeftCorner(GL3DVec2d newlLowerLeftCorner) {
        lowerLeftCorner = newlLowerLeftCorner;
    }

    @Override
    public GL3DVec2d getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    @Override
    public GL3DVec2d getPhysicalUpperLeft() {
        return new GL3DVec2d(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    @Override
    public GL3DVec2d getPhysicalSize() {
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
    public GL3DQuatd getRotationObs() {
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

}
