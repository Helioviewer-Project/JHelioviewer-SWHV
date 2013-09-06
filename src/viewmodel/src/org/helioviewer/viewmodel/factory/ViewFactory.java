package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.View;

/**
 * Factory to produce different views.
 * 
 * <p>
 * Since the viewmodel supports different types view chains (currently Java
 * BufferedImage and OpenGL), it is helpful to use a factory for creating views,
 * so the user only has to know the interface that should be used. The factory
 * is responsible for returning the correct implementation.
 * <p>
 * The factory should always be used for creating views, especially when
 * creating views using existing view as a pattern.
 * 
 * @author Markus Langenberg
 */
public interface ViewFactory {

    /**
     * Returns a new view implementation, given a interface.
     * 
     * @param <T>
     *            interface type of new view
     * @param pattern
     *            interface to search implementation for
     * @return class implementing the interface
     */
    public <T extends View> T createNewView(Class<T> pattern);

    /**
     * Returns a new view implementation, given an existing implementation.
     * 
     * The user can decide what should happen to the source:
     * <p>
     * If keepSource is false, the function copies all all functional members
     * from the source to new implementation, so after calling this function,
     * the source is still usable and untouched. If keeping the source is not
     * necessary (or even unmeant), use keepSource = false instead.
     * <p>
     * If keepSource is false, the function moves all functional members from
     * the source to new implementation, so after calling this function, the
     * source it not usable any more. If keeping the source is necessary, use
     * keepSource = true instead.
     * <p>
     * Note, that the new view is not connected any other views yet. This has to
     * be done by the caller.
     * 
     * @param <T>
     *            interface type of new view
     * @param source
     *            pattern to create new view
     * @param keepSource
     *            if true keeps the source untouched
     * @return implementation equivalent to source
     */
    public <T extends View> T createViewFromSource(T source, boolean keepSource);
}
