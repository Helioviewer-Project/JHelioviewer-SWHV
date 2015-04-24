package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.GL3DVec2d;

/**
 * Represents a region.
 *
 * A region describes an area in an image. BasicRegion holds the minimal needed
 * information to define a region.
 * <p>
 * A region usually will be scaled with the units per pixel (
 * {@link ImageSizeMetaData#getUnitsPerPixel}) value which is defined in the
 * meta data of an image. Apart from that, for solar images, the origin of
 * physical coordinate system is the center of the sun.
 *
 * @author Ludwig Schmidt
 * */
public interface BasicRegion {

    /**
     * Returns the position of the lower left corner of the region.
     *
     * @return a GL3DVec2d object which points to the lower left corner of the
     *         region.
     * */
    public GL3DVec2d getLowerLeftCorner();

    /**
     * Returns the size of the region.
     *
     * @return aGL3DVec2d object which describes the size of the region.
     * */
    public GL3DVec2d getSize();

}
