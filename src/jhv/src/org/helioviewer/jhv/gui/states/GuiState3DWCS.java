package org.helioviewer.jhv.gui.states;

import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.gui.GL3DTopToolBar;
import org.helioviewer.gl3d.model.GL3DInternalPluginConfiguration;
import org.helioviewer.gl3d.plugin.GL3DPluginController;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

public class GuiState3DWCS extends GuiState2D {

    public GuiState3DWCS() {
        // Override the viewchainFactory with a specific 3D implementation
        super(new GL3DViewchainFactory());
    }

    @Override
    public void activate() {
        super.activate();
        GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
        GL3DPluginController.getInstance().setPluginConfiguration(new GL3DInternalPluginConfiguration());
        GL3DPluginController.getInstance().loadPlugins();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void addStateSpecificComponents(SideContentPane sideContentPane) {
        GL3DCameraSelectorModel.getInstance();
    }

    @Override
    public void removeStateSpecificComponents(SideContentPane sideContentPane) {

    }

    @Override
    public ViewStateEnum getType() {
        return ViewStateEnum.View3D;
    }

    @Override
    public TopToolBar getTopToolBar() {
        if (topToolBar == null) {
            topToolBar = new GL3DTopToolBar();
        }
        return topToolBar;
    }

    @Override
    public ImagePanelInputController getDefaultInputController() {
        return new GL3DCameraMouseController();
    }

    @Override
    public boolean isOverviewPanelInteractionEnabled() {
        return false;
    }

}
