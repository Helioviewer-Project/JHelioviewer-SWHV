package org.helioviewer.jhv.plugins.swek.config;

/**
 * SWEK represenation of the spacial region.
 *
 * @author Bram Bourgoignie (bram.bourgoignie@oma.be)
 *
 */
public class SWEKSpatialRegion {

    /** x1 coordinate of the spatial region */
    public final int x1;
    /** y1 coordinate of the spatial region */
    public final int y1;
    /** x2 coordinate of the spatial region */
    public final int x2;
    /** y2 coordinate of the spatial region */
    public final int y2;

    /**
     * Constructs a spatial region with the coordinates (x1, y1) and (x2, y2).
     *
     * @param x1
     *            the x1 coordinate of the spatial region
     * @param y1
     *            the y1 coordinate of the spatial region
     * @param x2
     *            the x2 coordinate of the spatial region
     * @param y2
     *            the y2 coordinate of the spatial region
     */
    public SWEKSpatialRegion(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

}
