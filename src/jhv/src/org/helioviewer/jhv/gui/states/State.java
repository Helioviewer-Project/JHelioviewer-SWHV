package org.helioviewer.jhv.gui.states;

import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;

/**
 * State Interface
 *
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 *
 */
public interface State {

    /**
     * To get the type of the state
     *
     * @return ViewStateEnum type
     */
    public ViewStateEnum getType();

    public ImagePanelInputController getDefaultInputController();

}
