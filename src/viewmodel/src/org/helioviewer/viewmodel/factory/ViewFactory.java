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

}
