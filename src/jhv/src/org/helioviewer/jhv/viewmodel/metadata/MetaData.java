package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
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
    public Vec2d getPhysicalSize();

    /**
     * Returns the physical position of the lower left corner of the
     * corresponding image.
     *
     * @return Physical position of the lower left corner
     */
    public Vec2d getPhysicalLowerLeft();

    public Vec2d getPhysicalUpperLeft();

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

    public Quatd getRotationObs();

    public double getDistanceObs();

    public double getInnerCutOffRadius();

    public double getOuterCutOffRadius();

    float getCutOffValue();

    Vec3d getCutOffDirection();

}
