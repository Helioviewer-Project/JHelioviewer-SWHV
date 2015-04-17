package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;

/**
 * Extension of {@link BasicRegion}, representing a region.
 * 
 * It might be useful to get the basic information of a region in another way.
 * The methods provide a mapping of the basic values in different formats.
 * 
 * @author Ludwig Schmidt
 * */
public interface Region extends BasicRegion {

    /**
     * Returns the x coordinate of the lower left corner of the region.
     * 
     * @return x coordinate of the lower left corner.
     * */
    public double getCornerX();

    /**
     * Returns the y coordinate of the lower left corner of the region.
     * 
     * @return y coordinate of the lower left corner.
     * */
    public double getCornerY();

    /**
     * Returns the width of the region.
     * 
     * @return width of the region.
     * */
    public double getWidth();

    /**
     * Returns the height of the region.
     * 
     * @return height of the region.
     * */
    public double getHeight();

    /**
     * Returns a RectangleDouble object containing the basic region information.
     * 
     * @return a RectangleDouble object containing the basic region information.
     * */
    public RectangleDouble getRectangle();

    /**
     * Returns the position of the upper left corner of the region.
     * 
     * @return a Vector2dDouble object which points to the upper left corner of
     *         the region.
     * */
    public Vector2dDouble getUpperLeftCorner();

    /**
     * Returns the position of the lower right corner of the region.
     * 
     * @return a Vector2dDouble object which points to the lower right corner of
     *         the region.
     * */
    public Vector2dDouble getLowerRightCorner();

    /**
     * Returns the position of the upper right corner of the region.
     * 
     * @return a Vector2dDouble object which points to the upper right corner of
     *         the region.
     * */
    public Vector2dDouble getUpperRightCorner();

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o);
}
