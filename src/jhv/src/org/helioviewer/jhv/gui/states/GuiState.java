package org.helioviewer.jhv.gui.states;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.model.GL3DInternalPluginConfiguration;
import org.helioviewer.gl3d.plugin.GL3DPluginController;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

public class GuiState implements State {

    private final boolean is3d;

    private ComponentView mainComponentView;
    private RenderModeStatusPanel renderModeStatus;

    public GuiState(boolean is3d) {
        this.is3d = is3d;
    }

    @Override
    public boolean createViewChains() {
        Log.info("Start creating view chains");

        boolean firstTime = (mainComponentView == null);
        // Create main view chain
        GL3DViewchainFactory mainFactory = new GL3DViewchainFactory();
        mainComponentView = mainFactory.createViewchainMain(mainComponentView, false);
        GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
        GL3DPluginController.getInstance().setPluginConfiguration(new GL3DInternalPluginConfiguration());
        GL3DPluginController.getInstance().loadPlugins();
        return firstTime;
    }

    @Override
    public boolean recreateViewChains(State previousState) {
        Displayer.getSingletonInstance().removeListeners();

        if (previousState == null || previousState.getMainComponentView() == null) {
            return this.createViewChains();
        } else {
            mainComponentView = previousState.getMainComponentView();
            return false;
        }
    }

    @Override
    public ViewStateEnum getType() {
        if (is3d) {
            return ViewStateEnum.View3D;
        } else {
            return ViewStateEnum.View2D;
        }
    }

    @Override
    public ComponentView getMainComponentView() {
        return mainComponentView;
    }

    public RenderModeStatusPanel getRenderModeStatus() {
        return renderModeStatus;
    }

    @Override
    public ImagePanelInputController getDefaultInputController() {
        if (is3d) {
            return new GL3DCameraMouseController();
        } else {
            return new MainImagePanelMousePanController();
        }
    }

}
