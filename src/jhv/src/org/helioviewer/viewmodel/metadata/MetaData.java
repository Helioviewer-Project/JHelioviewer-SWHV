package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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
    public GL3DVec2d getPhysicalImageSize();

    /**
     * Returns the physical position of the lower left corner of the
     * corresponding image.
     *
     * @return Physical position of the lower left corner
     */
    public GL3DVec2d getPhysicalLowerLeft();

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
    public GL3DVec2d getPhysicalUpperLeft();

    /**
     * Returns the physical position of the lower right corner of the
     * corresponding image.
     *
     * @return Physical position of the lower right corner
     */
    public GL3DVec2d getPhysicalLowerRight();

    /**
     * Returns the physical position of the upper right corner of the
     * corresponding image.
     *
     * @return Physical position of the upper right corner
     */
    public GL3DVec2d getPhysicalUpperRight();

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

    public ImmutableDateTime getDateTime();

    public GL3DQuatd getLocalRotation();

}
