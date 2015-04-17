package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;

/**
 * Implementation of {@link BasicRegion}.
 * 
 * @author Ludwig Schmidt
 * */
public class StaticRegion implements BasicRegion {

    private final Vector2dDouble lowerLeftCorner;
    private final Vector2dDouble sizeVector;

    /**
     * Constructor where to pass the information as double values.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * */
    public StaticRegion(final double newLowerLeftX, final double newLowerLeftY, final double newWidth, final double newHeight) {
        lowerLeftCorner = new Vector2dDouble(newLowerLeftX, newLowerLeftY);
        sizeVector = new Vector2dDouble(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the lower left corner information as a vector
     * and the size of the region as double values.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * */
    public StaticRegion(final Vector2dDouble newLowerLeftCorner, final double newWidth, final double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vector2dDouble(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the left lower corner information as double
     * values and the size of the region as a Vector.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * */
    public StaticRegion(final double newLowerLeftX, final double newLowerLeftY, final Vector2dDouble newSizeVector) {
        lowerLeftCorner = new Vector2dDouble(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    /**
     * Constructor where to pass the left lower corner information and the size
     * of the region as a Vector.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * */
    public StaticRegion(final Vector2dDouble newLowerLeftCorner, final Vector2dDouble newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    /**
     * Constructor where to pass the region information by a rectangle.
     * 
     * @param newRectangle
     *            RectangleDouble object which represents the basic information
     *            of a region.
     * */
    public StaticRegion(final RectangleDouble newRectangle) {
        lowerLeftCorner = newRectangle.getLowerLeftCorner();
        sizeVector = newRectangle.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getSize() {
        return sizeVector;
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final Vector2dDouble newLowerLeftCorner, final Vector2dDouble newSizeVector) {
        return new RegionAdapter(new StaticRegion(newLowerLeftCorner, newSizeVector));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newCornerX
     *            x coordinate of lower left corner of the region.
     * @param newCornerY
     *            y coordinate of lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final double newCornerX, final double newCornerY, final double newWidth, final double newHeight) {
        return new RegionAdapter(new StaticRegion(newCornerX, newCornerY, newWidth, newHeight));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final double newLowerLeftX, final double newLowerLeftY, final Vector2dDouble newSizeVector) {
        return new RegionAdapter(new StaticRegion(newLowerLeftX, newLowerLeftY, newSizeVector));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final Vector2dDouble newLowerLeftCorner, final double newWidth, final double newHeight) {
        return new RegionAdapter(new StaticRegion(newLowerLeftCorner, newWidth, newHeight));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newRectangle
     *            RectangleDouble object which represents the basic information
     *            of a region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final RectangleDouble newRectangle) {
        return new RegionAdapter(new StaticRegion(newRectangle));
    }

}
