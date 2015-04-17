package org.helioviewer.viewmodel.viewportimagesize;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Implementation of {@link BasicViewportImageSize}.
 * 
 * @author Ludwig Schmidt
 * */
public class StaticViewportImageSize implements BasicViewportImageSize {

    private Vector2dInt sizeVector;

    /**
     * Constructor where to pass the image size information inside the viewport
     * as int values.
     * 
     * @param newWidth
     *            width of the image inside the viewport.
     * @param newHeight
     *            height of the image inside the viewport.
     * */
    public StaticViewportImageSize(final int newWidth, final int newHeight) {
        sizeVector = new Vector2dInt(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the viewport information as a vector.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the image
     *            inside the viewport.
     * */
    public StaticViewportImageSize(final Vector2dInt newSizeVector) {
        sizeVector = newSizeVector;
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dInt getSizeVector() {
        return sizeVector;
    }

    /**
     * Creates a ViewportImageSizeAdapter object by using the passed image size
     * information.
     * 
     * @param newWidth
     *            width of the image inside the viewport.
     * @param newHeight
     *            height of the image inside the viewport.
     * */
    public static ViewportImageSize createAdaptedViewportImageSize(final int newWidth, final int newHeight) {
        return new ViewportImageSizeAdapter(new StaticViewportImageSize(newWidth, newHeight));
    }

    /**
     * Creates a ViewportImageSizeAdapter object by using the passed image size
     * information.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the image
     *            inside the viewport.
     * */
    public static ViewportImageSize createAdaptedViewportImageSize(final Vector2dInt newSizeVector) {
        return new ViewportImageSizeAdapter(new StaticViewportImageSize(newSizeVector));
    }

}
