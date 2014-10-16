package org.helioviewer.jhv.gui;

import org.helioviewer.gl3d.factory.GL3DViewFactory;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.gl3d.view.GL3DComponentView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.view.GL3DViewportView;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.View;
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
        // Layered View
        LayeredView layeredView = viewFactory.createNewView(LayeredView.class);

        // This is where the 2D Viewchain is connected to the 3D part!
        // GL3DOrthoView orthoView =
        // viewFactory.createNewView(GL3DOrthoView.class);
        // orthoView.setView(layeredView);
        GLOverlayView overlayView = viewFactory.createNewView(GLOverlayView.class);
        overlayView.setView(layeredView);

        GL3DCameraView cameraView = viewFactory.createNewView(GL3DCameraView.class);
        cameraView.setView(overlayView);

        GL3DViewportView viewportView = viewFactory.createNewView(GL3DViewportView.class);
        viewportView.setView(cameraView);

        GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
        currentSceneGraph = sceneGraph;
        sceneGraph.setView(viewportView);
        sceneGraph.setGLOverlayView(overlayView);

        GL3DComponentView componentView = viewFactory.createNewView(GL3DComponentView.class);
        componentView.setView(sceneGraph);

        // add Overlays (OvwelayView added before LayeredView and after
        // GL3DCameraView)
        updateOverlayViewsInViewchainMain(overlayView);

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

        if (targetView != null && targetView.getClass().isAssignableFrom(GL3DComponentView.class)) {
            //View overlayView = sourceView.getAdapter(OverlayView.class);
            View layeredView = sourceView.getAdapter(LayeredView.class);

            ViewFactory viewFactory = getUsedViewFactory();
            View gl3dLayeredView = viewFactory.createViewFromSource(layeredView, false);

            for (int i = 0; i < ((LayeredView) layeredView).getNumLayers(); i++) {
                if (!((LayeredView) layeredView).isVisible(((LayeredView) layeredView).getLayer(i))) {
                    ((GL3DLayeredView) gl3dLayeredView).toggleVisibility(((GL3DLayeredView) gl3dLayeredView).getLayer(i));
                }
            }

            // GL3DOrthoView orthoView =
            // viewFactory.createNewView(GL3DOrthoView.class);
            // orthoView.setView(gl3dLayeredView);

            GLOverlayView oldOverlayView = sourceView.getAdapter(GLOverlayView.class);
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

            ((GL3DComponentView) targetView).setView(sceneGraph);

            // do this recursively and proper (call for every view in the 3d
            // view chain, maybee...)
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