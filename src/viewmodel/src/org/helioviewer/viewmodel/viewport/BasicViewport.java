package org.helioviewer.viewmodel.viewport;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Represents a viewport.
 * 
 * A viewport describes the size in pixel of the output medium (generally the
 * window where to display the image).
 * 
 * @author Ludwig Schmidt
 * */
public interface BasicViewport {

    /**
     * Returns the size of the viewport.
     * 
     * @return size of the viewport represented by a Vector2dInt object.
     * */
    public Vector2dInt getSize();

}
