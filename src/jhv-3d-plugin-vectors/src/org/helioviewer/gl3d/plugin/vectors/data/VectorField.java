package org.helioviewer.gl3d.plugin.vectors.data;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;

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
    public GL3DVec2d sizePixel;

    /**
     * the size of the field in Arcseconds.
     */
    public GL3DVec2d sizeArcsec;

    /**
     * the position of the upper left corner of the vector field (in Arcseconds)
     */
    public GL3DVec2d posArcsec;

    /**
     * A 2-dimensional Array representing the vectorfields. Time is the first
     * Dimension. And the second Dimension
     */
    public VectorData[][] vectors;
}
