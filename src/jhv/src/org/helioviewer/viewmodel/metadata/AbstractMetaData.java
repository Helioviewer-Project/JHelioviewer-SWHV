package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Abstract base class implementing MetaData.
 *
 * <p>
 * This class implements the all functions of meta data. The lower left corner
 * and the size are saved, which contains all information needed. The other
 * corners are calculates based on the lower left corner and the size.
 *
 * @author Ludwig Schmidt
 *
 */
public abstract class AbstractMetaData implements MetaData {

    private GL3DVec2d lowerLeftCorner;
    private GL3DVec2d sizeVector;
    protected ImmutableDateTime dateTime = null;

    /**
     * Default constructor, does not set size or position.
     */
    public AbstractMetaData() {
        lowerLeftCorner = null;
        sizeVector = null;
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCorner
     *            Physical lower left corner of the corresponding image
     * @param newSizeVector
     *            Physical size of the corresponding image
     */
    public AbstractMetaData(GL3DVec2d newLowerLeftCorner, GL3DVec2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
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
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCorner
     *            Physical lower left corner of the corresponding image
     * @param newWidth
     *            Physical width of the corresponding image
     * @param newHeight
     *            Physical height of the corresponding image
     */
    public AbstractMetaData(GL3DVec2d newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new GL3DVec2d(newWidth, newHeight);
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newLowerLeftCornerX
     *            Physical lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Physical lower left y-coordinate of the corresponding image
     * @param newSizeVector
     *            Physical size of the corresponding image
     */
    public AbstractMetaData(double newLowerLeftCornerX, double newLowerLeftCornerY, GL3DVec2d newSizeVector) {
        lowerLeftCorner = new GL3DVec2d(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = newSizeVector;
    }

    /**
     * Constructor, setting size and position.
     *
     * @param newRectangle
     *            Full physical rectangle of the corresponding image
     */
    public AbstractMetaData(RectangleDouble newRectangle) {
        lowerLeftCorner = newRectangle.getLowerLeftCorner();
        sizeVector = newRectangle.getSize();
    }

    /**
     * Copy constructor
     *
     * @param original
     *            Object to copy
     */
    public AbstractMetaData(AbstractMetaData original) {
        lowerLeftCorner = new GL3DVec2d(original.lowerLeftCorner);
        sizeVector = new GL3DVec2d(original.sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalSize() {
        return sizeVector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPhysicalHeight() {
        return sizeVector.y;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPhysicalWidth() {
        return sizeVector.x;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalLowerRight() {
        return GL3DVec2d.add(lowerLeftCorner, sizeVector.getXVector());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalUpperLeft() {
        return GL3DVec2d.add(lowerLeftCorner, sizeVector.getYVector());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalUpperRight() {
        return GL3DVec2d.add(lowerLeftCorner, sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RectangleDouble getPhysicalRectangle() {
        return new RectangleDouble(lowerLeftCorner, sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Region getPhysicalRegion() {
        return StaticRegion.createAdaptedRegion(lowerLeftCorner, sizeVector);
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
    public ImmutableDateTime getDateTime() {
        return this.dateTime;
    }

}
