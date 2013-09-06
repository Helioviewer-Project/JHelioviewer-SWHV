package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;

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

    private Vector2dDouble lowerLeftCorner;
    private Vector2dDouble sizeVector;

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
    public AbstractMetaData(Vector2dDouble newLowerLeftCorner, Vector2dDouble newSizeVector) {
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
        lowerLeftCorner = new Vector2dDouble(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = new Vector2dDouble(newWidth, newHeight);
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
    public AbstractMetaData(Vector2dDouble newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vector2dDouble(newWidth, newHeight);
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
    public AbstractMetaData(double newLowerLeftCornerX, double newLowerLeftCornerY, Vector2dDouble newSizeVector) {
        lowerLeftCorner = new Vector2dDouble(newLowerLeftCornerX, newLowerLeftCornerY);
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
        lowerLeftCorner = new Vector2dDouble(original.lowerLeftCorner);
        sizeVector = new Vector2dDouble(original.sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2dDouble getPhysicalImageSize() {
        return sizeVector;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2dDouble getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageHeight() {
        return sizeVector.getY();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageWidth() {
        return sizeVector.getX();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2dDouble getPhysicalLowerRight() {
        return lowerLeftCorner.add(sizeVector.getXVector());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2dDouble getPhysicalUpperLeft() {
        return lowerLeftCorner.add(sizeVector.getYVector());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2dDouble getPhysicalUpperRight() {
        return lowerLeftCorner.add(sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized RectangleDouble getPhysicalRectangle() {
        return new RectangleDouble(lowerLeftCorner, sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Region getPhysicalRegion() {
        return StaticRegion.createAdaptedRegion(lowerLeftCorner, sizeVector);
    }

    /**
     * Sets the physical size of the corresponding image.
     * 
     * @param newImageSize
     *            Physical size of the corresponding image
     */
    protected synchronized void setPhysicalImageSize(Vector2dDouble newImageSize) {
        sizeVector = newImageSize;
    }

    /**
     * Sets the physical lower left corner the corresponding image.
     * 
     * @param newlLowerLeftCorner
     *            Physical lower left corner the corresponding image
     */
    protected synchronized void setPhysicalLowerLeftCorner(Vector2dDouble newlLowerLeftCorner) {
        lowerLeftCorner = newlLowerLeftCorner;
    }

}
