package org.helioviewer.jhv.plugins.swek.config;

/**
 * SWEK represenation of the spacial region.
 * 
 * @author Bram Bourgoignie (bram.bourgoignie@oma.be)
 * 
 */
public class SWEKSpatialRegion {
    /** x1 coordinate of the spatial region */
    private int x1;
    /** y1 coordinate of the spatial region */
    private int y1;
    /** x2 coordinate of the spatial region */
    private int x2;
    /** y2 coordinate of the spatial region */
    private int y2;

    /**
     * Default constructor of the spatial region
     */
    public SWEKSpatialRegion() {
        this.x1 = 0;
        this.y1 = 0;
        this.x2 = 0;
        this.y2 = 0;
    }

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

    /**
     * Gets the x1 coordinate of the spatial region.
     * 
     * @return the x1 the x1 coordinate
     */
    public int getX1() {
        return this.x1;
    }

    /**
     * Sets the x1 coordinate of the spatial region.
     * 
     * @param x1
     *            the x1 to set
     */
    public void setX1(int x1) {
        this.x1 = x1;
    }

    /**
     * Gets the y1 coordinate of the spatial region.
     * 
     * @return the y1
     */
    public int getY1() {
        return this.y1;
    }

    /**
     * @param y1
     *            the y1 to set
     */
    public void setY1(int y1) {
        this.y1 = y1;
    }

    /**
     * @return the x2
     */
    public int getX2() {
        return this.x2;
    }

    /**
     * @param x2
     *            the x2 to set
     */
    public void setX2(int x2) {
        this.x2 = x2;
    }

    /**
     * @return the y2
     */
    public int getY2() {
        return this.y2;
    }

    /**
     * @param y2
     *            the y2 to set
     */
    public void setY2(int y2) {
        this.y2 = y2;
    }

}
