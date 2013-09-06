package org.helioviewer.viewmodel.viewportimagesize;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Represents the size of the image in the viewport.
 * 
 * The viewport image size describes the size in pixel of the area where image
 * data inside the viewport is available.
 * <p>
 * Generally the viewport image size has the same size as the viewport. But in
 * some cases the viewport image size might be smaller than the viewport, e.g.
 * when the aspact ratio of the image data is different to the aspect ratio of
 * the viewport.
 * 
 * @author Ludwig Schmidt
 * */
public interface BasicViewportImageSize {

    /**
     * Returns the size of the image inside the viewport.
     * 
     * @return a Vector2dInt object which describes the size of the image inside
     *         the viewport.
     * */
    public Vector2dInt getSizeVector();

}
