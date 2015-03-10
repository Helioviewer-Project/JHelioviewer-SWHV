package org.helioviewer.jhv.gui;

import org.helioviewer.viewmodel.factory.GL3DViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.opengl.GL3DCameraView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;

//import org.helioviewer.viewmodel.view.opengl.GL3DViewportView;

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

        //GL3DViewportView viewportView = viewFactory.createNewView(GL3DViewportView.class);
        //viewportView.setView(cameraView);

        GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
        currentSceneGraph = sceneGraph;
        sceneGraph.setView(/* viewportView */cameraView);
        sceneGraph.setGLOverlayView((GLOverlayView) overlayView);

        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        componentView.setView(sceneGraph);

        // add Overlays (OverlayView added before LayeredView and after GL3DCameraView)
        updateOverlayViewsInViewchainMain((GLOverlayView) overlayView);

        return componentView;
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