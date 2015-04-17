package org.helioviewer.viewmodel.viewportimagesize;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Implementation of {@link ViewportImageSize}.
 * 
 * @author Ludwig Schmidt
 * */
public class ViewportImageSizeAdapter implements ViewportImageSize {

    private final BasicViewportImageSize viewportImageSize;

    /**
     * Default constructor.
     * 
     * @param newViewportImageSize
     *            BasicViewportImageSize object which holds the image size
     *            description inside the viewport.
     * */
    public ViewportImageSizeAdapter(final BasicViewportImageSize newViewportImageSize) {
        viewportImageSize = newViewportImageSize;
    }

    /**
     * {@inheritDoc}
     * */
    public int getHeight() {
        return viewportImageSize.getSizeVector().getY();
    }

    /**
     * {@inheritDoc}
     * */
    public int getWidth() {
        return viewportImageSize.getSizeVector().getX();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dInt getSizeVector() {
        return viewportImageSize.getSizeVector();
    }

    /**
     * {@inheritDoc}
     * */
    public boolean hasArea() {
        return viewportImageSize.getSizeVector().getX() != 0 && viewportImageSize.getSizeVector().getY() != 0;
    }

}
