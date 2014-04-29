package org.helioviewer.jhv.gui.states;

import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.gui.GL3DModelPanel;
import org.helioviewer.gl3d.gui.GL3DTopToolBar;
import org.helioviewer.gl3d.model.GL3DInternalPluginConfiguration;
import org.helioviewer.gl3d.plugin.GL3DPluginController;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

public class GuiState3DWCS extends GuiState2D {

    // private JPanel gl3dSettingsPanel;

    private GL3DModelPanel modelPanel;

    public GuiState3DWCS() {
        // Override the viewchainFactory with a specific 3D implementation
        super(new GL3DViewchainFactory());
    }

    public void activate() {
        super.activate();
        GLTextureHelper.is2DState = false;
        GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
        GL3DPluginController.getInstance().setPluginConfiguration(new GL3DInternalPluginConfiguration());
        GL3DPluginController.getInstance().loadPlugins();
    }

    public void deactivate() {
        super.deactivate();
        GLTextureHelper.is2DState = true;
    }

    /*
     * private void createSettingsPanel() { gl3dSettingsPanel = new JPanel(new
     * GridBagLayout());
     * 
     * GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
     * GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0),
     * 0, 0);
     * 
     * c.weightx = 0.5; gl3dSettingsPanel.add(new JLabel("Camera"), c); c.gridx
     * = 1; c.weightx = 1.0; gl3dSettingsPanel.add(new
     * GL3DCameraSelectorPanel(), c); c.gridx = 0; c.gridy++;
     * 
     * c.gridwidth=2; gl3dSettingsPanel.add(new JLabel("Models"), c); c.gridy++;
     * 
     * JTabbedPane tabs = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
     * 
     * GL3DModelSelectorPanel gl3DModelSelectorPanel = new
     * GL3DModelSelectorPanel(); tabs.add("Models", gl3DModelSelectorPanel);
     * tabs.add("Scene Browser", new GL3DSceneGraphPanel());
     * gl3dSettingsPanel.add(tabs, c); }
     */

    public void addStateSpecificComponents(SideContentPane sideContentPane) {
        GL3DCameraSelectorModel.getInstance();
        if (this.modelPanel == null) {
            this.modelPanel = new GL3DModelPanel();
        }
        modelPanel.init();
        sideContentPane.add("Model Plugins", this.modelPanel, false);
    }

    public void removeStateSpecificComponents(SideContentPane sideContentPane) {
        sideContentPane.remove(this.modelPanel);
        this.modelPanel.destroy();
    }

    public ViewStateEnum getType() {
        return ViewStateEnum.View3D;
    }

    public TopToolBar getTopToolBar() {
        if (topToolBar == null) {
            topToolBar = new GL3DTopToolBar();
        }
        return topToolBar;
    }

    public ImagePanelInputController getDefaultInputController() {
        return new GL3DCameraMouseController();
    }

    public boolean isOverviewPanelInteractionEnabled() {
        return false;
    }
}
