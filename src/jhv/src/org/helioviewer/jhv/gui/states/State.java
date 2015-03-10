package org.helioviewer.jhv.gui.states;

import org.helioviewer.jhv.gui.GL3DViewchainFactory;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

/**
 * State Interface
 *
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 *
 */
public interface State {

    /**
     * The ViewchainFactory is dependent on the state, as a different Viewchain
     * is required for 3D and 2D Modes.
     *
     * @return viewchainFactory to use
     */
    public GL3DViewchainFactory getViewchainFactory();

    /**
     * To get the type of the state
     *
     * @return ViewStateEnum type
     */
    public ViewStateEnum getType();

    public ImagePanelInputController getDefaultInputController();

}
