package org.helioviewer.jhv.gui.states;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.gui.GL3DTopToolBar;
import org.helioviewer.gl3d.model.GL3DInternalPluginConfiguration;
import org.helioviewer.gl3d.plugin.GL3DPluginController;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.controller.MainImagePanelMousePanController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;


public class GuiState implements State {

    private final boolean is3d;
    private final ViewchainFactory viewchainFactory;

    private TopToolBar topToolBar;
    private ComponentView mainComponentView;
    private RenderModeStatusPanel renderModeStatus;

    public GuiState(ViewchainFactory viewchainFactory) {
        this.viewchainFactory = viewchainFactory;

        if (viewchainFactory instanceof GL3DViewchainFactory) {
            is3d = true;
        } else {
            is3d = false;
        }
    }

    @Override
    public void addStateSpecificComponents(SideContentPane sideContentPane) {
        if (is3d) {
            GL3DCameraSelectorModel.getInstance();
        }
    }

    @Override
    public void removeStateSpecificComponents(SideContentPane sideContentPane) {
    }

    @Override
    public void activate() {
        if (is3d) {
            GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
            GL3DPluginController.getInstance().setPluginConfiguration(new GL3DInternalPluginConfiguration());
            GL3DPluginController.getInstance().loadPlugins();
        }
    }

    @Override
    public void deactivate() {
        getMainComponentView().deactivate();
    }

    @Override
    public boolean createViewChains() {
        Log.info("Start creating view chains");

        boolean firstTime = (mainComponentView == null);
        // Create main view chain
        ViewchainFactory mainFactory = this.viewchainFactory;
        mainComponentView = mainFactory.createViewchainMain(mainComponentView, false);

        return firstTime;
    }

    @Override
    public boolean recreateViewChains(State previousState) {
        Displayer.getSingletonInstance().removeListeners();

        if (previousState == null || previousState.getMainComponentView() == null) {
            return this.createViewChains();
        } else {
            mainComponentView = getViewchainFactory().createViewchainFromExistingViewchain(previousState.getMainComponentView(), this.mainComponentView, false);
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
    public TopToolBar getTopToolBar() {
        if (topToolBar == null) {
            if (is3d) {
                topToolBar = new GL3DTopToolBar();
            } else {
                topToolBar = new TopToolBar();
            }
        }

        return topToolBar;
    }

    @Override
    public ComponentView getMainComponentView() {
        return mainComponentView;
    }

    public RenderModeStatusPanel getRenderModeStatus() {
        return renderModeStatus;
    }

    @Override
    public ViewchainFactory getViewchainFactory() {
        return this.viewchainFactory;
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
