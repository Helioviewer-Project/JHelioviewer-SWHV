package org.helioviewer.jhv.data.datatype;

/**
 * Represents a point in a coordinate system.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVPoint {
    /** Coordinate 1 */
    private final Double coordinate1;
    /** Coordinate 2 */
    private final Double coordinate2;
    /** Coordinate 3 */
    private final Double coordinate3;

    /**
     * Creates a JHVPoint with the given coordinates.
     * 
     * @param coordinate1
     *            first coordinate
     * @param coordinate2
     *            second coordinate
     * @param coordinate3
     *            third coordinate
     */
    public JHVPoint(Double coordinate1, Double coordinate2, Double coordinate3) {
        this.coordinate1 = coordinate1;
        this.coordinate2 = coordinate2;
        this.coordinate3 = coordinate3;
    }

    /**
     * Gets first coordinate.
     * 
     * @return the first coordinate
     */
    public Double getCoordinate1() {
        return coordinate1;
    }

    /**
     * Gets the second coordinate.
     * 
     * @return the second coordinate
     */
    public Double getCoordinate2() {
        return coordinate2;
    }

    /**
     * Gets the third coordinate.
     * 
     * @return the third coordinate
     */
    public Double getCoordinate3() {
        return coordinate3;
    }
}
