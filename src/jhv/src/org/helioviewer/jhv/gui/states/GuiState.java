package org.helioviewer.jhv.gui.states;

import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

public class GuiState implements State {

    private final boolean is3d;
    GL3DCameraMouseController mouseController = new GL3DCameraMouseController();

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
    public ImagePanelInputController getDefaultInputController() {
        return mouseController;
    }
}
