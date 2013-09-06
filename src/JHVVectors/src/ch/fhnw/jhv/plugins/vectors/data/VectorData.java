package ch.fhnw.jhv.plugins.vectors.data;

/**
 * This Class represents one Vector with its Azimuth, Inclination and Length
 * (Strength).
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 27.06.2011
 * 
 */
public class VectorData implements Cloneable {

    /**
     * Azimuth angle
     */
    public float azimuth;

    /**
     * Inclination angle
     */
    public float inclination;

    /**
     * Length or strength of a vector
     */
    public float length;

    /**
     * X Coordinate position
     */
    public float x;

    /**
     * Y Coordinate position
     */
    public float y;

    /**
     * To string
     * 
     * @return String
     */

    public String toString() {
        return "[" + x + "][" + y + "] --> AZZE: " + azimuth + " INCLI: " + inclination + " LENGTH: " + length;
    }

    /**
     * Clone a VectorData Object
     * 
     * @return VectorData returns the new cloned VectorData object
     */

    public VectorData clone() {
        try {
            return (VectorData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen (Effective Java, Item
                                        // 11)
        }
    }
}
