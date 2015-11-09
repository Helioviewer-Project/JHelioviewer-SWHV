package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;

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
    public Vec2 getPhysicalSize();

    /**
     * Returns the physical position of the lower left corner of the
     * corresponding image.
     *
     * @return Physical position of the lower left corner
     */
    public Vec2 getPhysicalLowerLeft();

    public Vec2 getPhysicalUpperLeft();

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

    public JHVDate getDateObs();

    public Quat getRotationObs();

    public double getDistanceObs();

    public double getInnerCutOffRadius();

    public double getOuterCutOffRadius();

    float getCutOffValue();

    Vec3 getCutOffDirection();

}
