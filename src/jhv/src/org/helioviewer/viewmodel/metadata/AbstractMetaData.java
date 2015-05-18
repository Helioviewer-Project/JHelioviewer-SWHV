package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.jhv.display.Displayer;

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
    protected ImmutableDateTime dateTime = Displayer.epochDateTime;

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
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    @Override
    public GL3DVec2d getPhysicalUpperLeft() {
        return new GL3DVec2d(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GL3DVec2d getPhysicalSize() {
        return sizeVector;
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
        return dateTime;
    }

}
