package ch.fhnw.jhv.plugins.vectors.data;

import javax.vecmath.Vector2f;

/**
 * A Vectorfield represents a time-dependent set of vector-fields
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 27.06.2011
 * 
 */
public class VectorField {

    /**
     * the size of the field in pixel in the input image
     */
    public Vector2f sizePixel;

    /**
     * the size of the field in Arcseconds.
     */
    public Vector2f sizeArcsec;

    /**
     * the position of the upper left corner of the vector field (in Arcseconds)
     */
    public Vector2f posArcsec;

    /**
     * A 2-dimensional Array representing the vectorfields. Time is the first
     * Dimension. And the second Dimension
     */
    public VectorData[][] vectors;
}
