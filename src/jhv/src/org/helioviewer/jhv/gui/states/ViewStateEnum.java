package org.helioviewer.jhv.gui.states;

import org.helioviewer.jhv.gui.GL3DViewchainFactory;

/**
 * Contains the existing Gui States (2D, 3D)
 *
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 *
 */
public enum ViewStateEnum {

    View2D(new GuiState(new GL3DViewchainFactory(), false)), View3D(new GuiState(new GL3DViewchainFactory(), true));

    private final State state;

    ViewStateEnum(State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

}
