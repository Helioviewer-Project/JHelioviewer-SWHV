package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.GL3DCameraView;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GL3DLayeredView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;
//import org.helioviewer.viewmodel.view.opengl.GL3DViewportView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;

/**
 * The {@link ViewFactory} is responsible for creating new {@link View}s. The
 * views in 3D differs from the views in 2D, which is why a special 3D View
 * Factory is required. The {@link ViewFactory} is provided by the
 * {@link ViewchainFactory}
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DViewFactory extends GLViewFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends View> T createNewView(Class<T> pattern) {
        if (pattern.isAssignableFrom(GL3DSceneGraphView.class)) {
            return (T) new GL3DSceneGraphView();
        } else if (pattern.isAssignableFrom(GL3DCameraView.class)) {
            return (T) new GL3DCameraView();
        /* } else if (pattern.isAssignableFrom(GL3DViewportView.class)) {
            return (T) new GL3DViewportView(); */
        } else if (pattern.isAssignableFrom(GL3DImageTextureView.class)) {
            return (T) new GL3DImageTextureView();
        } else if (pattern.isAssignableFrom(ComponentView.class)) {
            return (T) new GL3DComponentView();
        } else if (pattern.isAssignableFrom(LayeredView.class)) {
            return (T) new GL3DLayeredView();
        } else if (pattern.isAssignableFrom(OverlayView.class)) {
            return (T) new GLOverlayView();
        } else {
            return super.createNewView(pattern);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T extends View> T createViewFromSourceImpl(T source) {
        // Check if a GL3DView is requested
        if (source instanceof LayeredView) {
            return (T) createLayeredViewFromExisting((LayeredView) source);
        } else if (source instanceof GL3DView) {
            if (source instanceof GL3DSceneGraphView) {
                return (T) new GL3DSceneGraphView();
            } else if (source instanceof GL3DCameraView) {
                return (T) new GL3DCameraView();
            /* } else if (source instanceof GL3DViewportView) {
                return (T) new GL3DViewportView(); */
            } else if (source instanceof GL3DImageTextureView) {
                return (T) new GL3DImageTextureView();
            } else {
                throw new IllegalArgumentException("Cannot create View from Source " + source);
            }
        } else if (source instanceof ComponentView) {
            return (T) new GL3DComponentView();
        } else {
            return super.createViewFromSourceImpl(source);
        }
    }

    /**
     * Creates a 3D layered view and copies the existing layers to the new view.
     *
     * @param layeredView
     *            old LayeredView
     * @return new GL3DLayeredView
     */
    private GL3DLayeredView createLayeredViewFromExisting(LayeredView layeredView) {
        GL3DLayeredView gl3dLayeredView = new GL3DLayeredView();

        for (int i = 0; i < layeredView.getNumLayers(); i++) {
            if (layeredView.getLayer(i) != null)
                gl3dLayeredView.addLayer(layeredView.getLayer(i));
        }
        return gl3dLayeredView;
    }
}
