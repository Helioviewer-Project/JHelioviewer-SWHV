package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;

/**
 * The {@link ViewFactory} is responsible for creating new {@link View}s. The
 * views in 3D differs from the views in 2D, which is why a special 3D View
 * Factory is required. The {@link ViewFactory} is provided by the
 * {@link ViewchainFactory}
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DViewFactory implements ViewFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends View> T createNewView(Class<T> pattern) {
        if (pattern.isAssignableFrom(ComponentView.class)) {
            return (T) new GL3DComponentView();
        } else {
            return null;
        }
    }

}
