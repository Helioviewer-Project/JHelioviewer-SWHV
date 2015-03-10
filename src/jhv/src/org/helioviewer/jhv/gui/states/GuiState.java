package org.helioviewer.jhv.gui.states;

import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

public class GuiState implements State {

    private final boolean is3d;
    public static GL3DViewchainFactory viewchainFactory = new GL3DViewchainFactory();

    private RenderModeStatusPanel renderModeStatus;

    public GuiState(boolean is3d) {
        this.is3d = is3d;
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
    public GL3DViewchainFactory getViewchainFactory() {
        return viewchainFactory;
    }

    @Override
    public ImagePanelInputController getDefaultInputController() {
        if (is3d) {
            return new GL3DCameraMouseController();
        } else {
            return new GL3DCameraMouseController();
        }
    }

}
