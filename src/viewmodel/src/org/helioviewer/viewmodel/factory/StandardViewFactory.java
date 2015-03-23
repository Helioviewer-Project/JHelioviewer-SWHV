package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.View;

/**
 * Abstract implementation of interface ViewFactory for independent views.
 *
 * <p>
 * This class produced views, which are independent from the type of the used
 * view chain and which can be used in every type. Since the set of views
 * created by this factory is incomplete, it does not make sense to use it
 * itself, thus it is abstract.
 * <p>
 * Apart from that, it provides some basic internal functionality.
 * <p>
 * For further details on how to use view factories, see {@link ViewFactory}.
 *
 * @author Markus Langenberg
 */
public abstract class StandardViewFactory implements ViewFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public abstract <T extends View> T createNewView(Class<T> pattern);

}
