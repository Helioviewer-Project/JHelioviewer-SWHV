package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.region.Region;

/**
 * Basic interface for meta data.
 * 
 * <p>
 * Meta data provide additional information about an image, such as its
 * resolution, its original physical size or the instrument, which took the
 * original photo.
 * 
 * <p>
 * Every meta data object should provide information about the original size of
 * the image.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface MetaData {

    /**
     * Returns the physical image size of the corresponding image.
     * 
     * @return Physical image size
     */
    public Vector2dDouble getPhysicalImageSize();

    /**
     * Returns the physical position of the lower left corner of the
     * corresponding image.
     * 
     * @return Physical position of the lower left corner
     */
    public Vector2dDouble getPhysicalLowerLeft();

    /**
     * Returns the physical image width of the corresponding image.
     * 
     * @return Physical image width
     */
    public double getPhysicalImageWidth();

    /**
     * Returns the physical image height of the corresponding image.
     * 
     * @return Physical image height
     */
    public double getPhysicalImageHeight();

    /**
     * Returns the physical position of the upper left corner of the
     * corresponding image.
     * 
     * @return Physical position of the upper left corner
     */
    public Vector2dDouble getPhysicalUpperLeft();

    /**
     * Returns the physical position of the lower right corner of the
     * corresponding image.
     * 
     * @return Physical position of the lower right corner
     */
    public Vector2dDouble getPhysicalLowerRight();

    /**
     * Returns the physical position of the upper right corner of the
     * corresponding image.
     * 
     * @return Physical position of the upper right corner
     */
    public Vector2dDouble getPhysicalUpperRight();

    /**
     * Returns the complete physical rectangle of the corresponding image.
     * 
     * @return Physical rectangle
     */
    public RectangleDouble getPhysicalRectangle();

    /**
     * Returns the physical rectangle as a region of the corresponding image.
     * 
     * @return Physical region
     */
    public Region getPhysicalRegion();
}
