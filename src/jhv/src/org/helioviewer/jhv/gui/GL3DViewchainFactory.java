package org.helioviewer.jhv.gui;

import org.helioviewer.viewmodel.factory.GL3DViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;

public class GL3DViewchainFactory {

    private final ViewFactory viewFactory;

    public GL3DViewchainFactory() {
        this.viewFactory = new GL3DViewFactory();
    }

    public ViewFactory getUsedViewFactory() {
        return viewFactory;
    }

    /**
     * This method creates the main view chain depending on the selected mode.
     *
     * The mode was defined when the instance of the class was created.
     * <p>
     * If the passed parameter value is null a new main view chain will be
     * created. If the passed parameter value represents a
     * {@link org.helioviewer.viewmodel.view.ComponentView} instance (the
     * topmost view of the view chain) the existing view chain will be
     * transfered with all its settings to a new view chain.
     *
     * @param currentMainImagePanelView
     *            instance of the ComponentView which is the topmost view of the
     *            view chain which has to be transfered to the new one.
     * @param keepSource
     *            If true, the source view chain stays untouched, otherwise it
     *            will be unusable afterwards
     * @return instance of the ComponentView of the new created view chain.
     */

    public ComponentView createViewchainMain(GL3DComponentView currentMainImagePanelView, boolean keepSource) {
        return createNewViewchainMain();
    }

    /**
     * Adds a new ImageInfoView to the main view chain and creates the
     * corresponding user interface components.
     *
     * The ImageInfoViews are always the undermost views in the view chain so
     * they will be added as a new layer to the LayeredView. For this reason
     * this method creates also the complete sub view chain (including the
     * needed filter views) and add it to the LayeredView.
     * <p>
     * If one of the passed parameter values is null nothing will happen.
     *
     * @param newLayer
     *            new ImageInfoView for which to create the sub view chain as a
     *            new layer.
     * @param attachToViewchain
     *            a view of the main view chain which is equal or over the
     *            LayeredView.
     */

    public void addLayerToViewchainMain(JHVJP2View newLayer) {
        if (newLayer == null)
            return;

    }

    protected ComponentView createNewViewchainMain() {

        ComponentView componentView = viewFactory.createNewView(ComponentView.class);
        //componentView.setView(cameraView);

        return componentView;
    }

}
