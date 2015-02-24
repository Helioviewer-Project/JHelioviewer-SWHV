package org.helioviewer.jhv.gui;

import java.util.AbstractList;

import org.helioviewer.gl3d.factory.GL3DViewFactory;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.view.GL3DViewportView;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;

public class GL3DViewchainFactory extends ViewchainFactory {
    public static GL3DSceneGraphView currentSceneGraph;

    public GL3DViewchainFactory() {
        super(new GL3DViewFactory());
    }

    /**
     * Creates a new main view chain with the minimal needed views.
     *
     * @return a instance of a ComponentView which is the topmost view of the
     *         new chain.
     */

    @Override
    protected ComponentView createNewViewchainMain() {
        ViewFactory viewFactory = getUsedViewFactory();

        LayeredView layeredView = viewFactory.createNewView(LayeredView.class);

        OverlayView overlayView = viewFactory.createNewView(OverlayView.class);
        overlayView.setView(layeredView);

        GL3DCameraView cameraView = viewFactory.createNewView(GL3DCameraView.class);
        cameraView.setView(overlayView);

        GL3DViewportView viewportView = viewFactory.createNewView(GL3DViewportView.class);
        viewportView.setView(cameraView);

        GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
        currentSceneGraph = sceneGraph;
        sceneGraph.setView(viewportView);
        sceneGraph.setGLOverlayView((GLOverlayView) overlayView);

        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        componentView.setView(sceneGraph);

        // add Overlays (OverlayView added before LayeredView and after GL3DCameraView)
        updateOverlayViewsInViewchainMain((GLOverlayView) overlayView);

        return componentView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentView createViewchainFromExistingViewchain(ComponentView sourceImagePanelView, ComponentView mainImagePanelView, boolean keepSource) {
        ViewFactory viewFactory = getUsedViewFactory();
        sourceImagePanelView.getAdapter(StandardSolarRotationTrackingView.class).setEnabled(false);
        ComponentView newView = viewFactory.createViewFromSource(sourceImagePanelView, keepSource);
        createViewchainFromExistingViewchain(sourceImagePanelView.getView(), newView, mainImagePanelView, keepSource);

        return newView;
    }

    @Override
    protected void createViewchainFromExistingViewchain(View sourceView, View targetView, ComponentView mainImagePanelView, boolean keepSource) {
        AbstractList<ViewListener> listeners = sourceView.getAllViewListener();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i) instanceof View) {
                sourceView.removeViewListener(listeners.get(i));
            }
        }
        StandardSolarRotationTrackingView solv = (StandardSolarRotationTrackingView) (((GLOverlayView) sourceView).getView());
        listeners = solv.getAllViewListener();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i) instanceof View) {
                solv.removeViewListener(listeners.get(i));
            }
        }
        solv.setRegion(null, null);

        GLLayeredView layeredView = sourceView.getAdapter(GLLayeredView.class);
        listeners = layeredView.getAllViewListener();

        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i) instanceof View) {
                layeredView.removeViewListener(listeners.get(i));
            }
        }
        if (targetView != null) {

            ViewFactory viewFactory = getUsedViewFactory();
            View gl3dLayeredView = viewFactory.createViewFromSource(layeredView, false);

            for (int i = 0; i < layeredView.getNumLayers(); i++) {
                View vv = layeredView.getLayer(i);
                vv.removeViewListener(layeredView);
                if (!layeredView.isVisible(layeredView.getLayer(i))) {

                    ((GL3DLayeredView) gl3dLayeredView).toggleVisibility(((GL3DLayeredView) gl3dLayeredView).getLayer(i));
                }
            }

            OverlayView oldOverlayView = sourceView.getAdapter(OverlayView.class);
            GLOverlayView overlayView = new GLOverlayView();
            overlayView.setOverlays(oldOverlayView.getOverlays());
            overlayView.setView(gl3dLayeredView);

            GL3DCameraView cameraView = viewFactory.createNewView(GL3DCameraView.class);
            cameraView.setView(overlayView);

            GL3DViewportView viewportView = viewFactory.createNewView(GL3DViewportView.class);
            viewportView.setView(cameraView);

            GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
            sceneGraph.setView(viewportView);
            sceneGraph.setGLOverlayView(overlayView);

            ((ComponentView) targetView).setView(sceneGraph);

        } else {
            super.createViewchainFromExistingViewchain(sourceView, targetView, mainImagePanelView, keepSource);
        }
    }

    @Override
    protected View getViewNextToOverlayView(ComponentView componentView) {
        return componentView.getAdapter(LayeredView.class);
    }

    @Override
    protected ModifiableInnerViewView getViewBeforeToOverlayView(ComponentView componentView) {
        return componentView.getAdapter(GL3DCameraView.class);
        // return componentView;
    }

}