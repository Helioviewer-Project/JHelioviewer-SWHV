package org.helioviewer.viewmodel.viewport;

import org.helioviewer.base.math.Vector2dInt;

/**
 * Implementation of {@link BasicViewport}.
 * 
 * @author Ludwig Schmidt
 * */
public class StaticViewport implements BasicViewport {

    private final Vector2dInt sizeVector;

    /**
     * Constructor where to pass the viewport information as int values.
     * 
     * @param newViewportWidth
     *            width of the viewport.
     * @param newViewportHeight
     *            height of the viewport.
     * */
    public StaticViewport(final int newViewportWidth, final int newViewportHeight) {
        sizeVector = new Vector2dInt(newViewportWidth, newViewportHeight);
    }

    /**
     * Constructor where to pass the viewport information as a vector.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the
     *            viewport.
     * */
    public StaticViewport(final Vector2dInt newSizeVector) {
        sizeVector = newSizeVector;
    }

    /**
     * Creates a ViewportAdapter object by using the passed viewport
     * information.
     * 
     * @param newViewportWidth
     *            width of the viewport.
     * @param newViewportHeight
     *            height of the viewport.
     * */
    public static Viewport createAdaptedViewport(final int newViewportWidth, final int newViewportHeight) {
        return new ViewportAdapter(new StaticViewport(newViewportWidth, newViewportHeight));
    }

    /**
     * Creates a ViewportAdapter object by using the passed viewport
     * information.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the
     *            viewport.
     * */
    public static Viewport createAdaptedViewport(final Vector2dInt newSizeVector) {
        return new ViewportAdapter(new StaticViewport(newSizeVector));
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dInt getSize() {
        return sizeVector;
    }

    public String toString() {
        return "[Viewport: Size: " + sizeVector + "]";
    }
}
