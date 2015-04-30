package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.GL3DVec2d;

/**
 * Implementation of {@link BasicRegion}.
 *
 * @author Ludwig Schmidt
 * */
public class StaticRegion implements BasicRegion {

    private final GL3DVec2d lowerLeftCorner;
    private final GL3DVec2d sizeVector;

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
        lowerLeftCorner = new GL3DVec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = new GL3DVec2d(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the lower left corner information as a vector
     * and the size of the region as double values.
     *
     * @param newLowerLeftCorner
     *            GL3DVec2d object which describes the position of the lower
     *            left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * */
    public StaticRegion(final GL3DVec2d newLowerLeftCorner, final double newWidth, final double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new GL3DVec2d(newWidth, newHeight);
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
     *            GL3DVec2d object which describes the size of the region.
     * */
    public StaticRegion(final double newLowerLeftX, final double newLowerLeftY, final GL3DVec2d newSizeVector) {
        lowerLeftCorner = new GL3DVec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    /**
     * Constructor where to pass the left lower corner information and the size
     * of the region as a Vector.
     *
     * @param newLowerLeftCorner
     *            GL3DVec2d object which describes the position of the lower
     *            left corner of the region.
     * @param newSizeVector
     *            GL3DVec2d object which describes the size of the region.
     * */
    public StaticRegion(final GL3DVec2d newLowerLeftCorner, final GL3DVec2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getSize() {
        return sizeVector;
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     *
     * @param newLowerLeftCorner
     *            GL3DVec2d object which describes the position of the lower
     *            left corner of the region.
     * @param newSizeVector
     *            GL3DVec2d object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final GL3DVec2d newLowerLeftCorner, final GL3DVec2d newSizeVector) {
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
     *            GL3DVec2d object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final double newLowerLeftX, final double newLowerLeftY, final GL3DVec2d newSizeVector) {
        return new RegionAdapter(new StaticRegion(newLowerLeftX, newLowerLeftY, newSizeVector));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     *
     * @param newLowerLeftCorner
     *            GL3DVec2d object which describes the position of the lower
     *            left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final GL3DVec2d newLowerLeftCorner, final double newWidth, final double newHeight) {
        return new RegionAdapter(new StaticRegion(newLowerLeftCorner, newWidth, newHeight));
    }

}
