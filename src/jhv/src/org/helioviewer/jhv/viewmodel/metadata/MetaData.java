package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.GL3DQuatd;
import org.helioviewer.jhv.base.math.GL3DVec2d;
import org.helioviewer.jhv.base.math.GL3DVec3d;
import org.helioviewer.jhv.base.time.ImmutableDateTime;

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

    public GL3DVec2d getPhysicalUpperLeft();

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

    public ImmutableDateTime getDateObs();

    public GL3DQuatd getRotationObs();

    public double getDistanceObs();

    public double getInnerCutOffRadius();

    public double getOuterCutOffRadius();

    float getCutOffValue();

    GL3DVec3d getCutOffDirection();

}
