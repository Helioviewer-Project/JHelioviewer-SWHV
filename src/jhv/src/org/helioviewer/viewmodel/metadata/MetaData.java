package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
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
    public GL3DVec2d getPhysicalSize();
    /**
     * Returns the physical position of the lower left corner of the
     * corresponding image.
     *
     * @return Physical position of the lower left corner
     */
    public GL3DVec2d getPhysicalLowerLeft();
    /**
     * Returns the width of the image in pixels.
     *
     * @return width of the image in pixels
     */
    public int getPixelWidth();

    /**
     * Returns the height of the image in pixels.
     *
     * @return height of the image in pixels
     */
    public int getPixelHeight();
    /**
     * Returns the conversion factor from pixels to a physical unit.
     *
     * @return conversion factor from pixels to a physical unit.
     */
    public double getUnitsPerPixel();

    public ImmutableDateTime getDateTime();

    public GL3DQuatd getLocalRotation();

}
