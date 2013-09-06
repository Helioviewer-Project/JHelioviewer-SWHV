package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.math.Vector2dDouble;

/**
 * Meta data providing information about the position and size of the sun in the
 * image.
 * 
 * <p>
 * This interface only makes sense for solar images. It provides informations
 * about size and position of the sun within the image in pixel coordinates. Of
 * course, the physical size and position is constant: The position is always
 * (0,0), the radius can be found at
 * {@link org.helioviewer.base.physics.Constants}.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface SunMetaData {

    /**
     * Returns the radius of the sun in pixels within the corresponding image.
     * 
     * @return Radius of the sun in pixels within the corresponding image
     */
    public double getSunPixelRadius();

    /**
     * Returns the position of the sun in pixels within the corresponding image.
     * 
     * @return Position of the sun in pixels within the corresponding image
     */
    public Vector2dDouble getSunPixelPosition();

}
