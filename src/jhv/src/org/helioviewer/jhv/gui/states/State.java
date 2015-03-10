package org.helioviewer.jhv.gui.states;

import org.helioviewer.jhv.gui.ViewchainFactory;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * State Interface
 *
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 *
 */
public interface State {

    public boolean createViewChains();

    /**
     * Create a new Viewchain based on the already existing view chain from the
     * previous state. Using this method ensures the reusage of common views
     * within the view chains.
     *
     * @param previousState
     *            State that was previously active
     * @return Sucess
     */
    public boolean recreateViewChains(State previousState);

    /**
     * The ViewchainFactory is dependent on the state, as a different Viewchain
     * is required for 3D and 2D Modes.
     *
     * @return viewchainFactory to use
     */
    public ViewchainFactory getViewchainFactory();

    /**
     * To get the type of the state
     *
     * @return ViewStateEnum type
     */
    public ViewStateEnum getType();

    public ComponentView getMainComponentView();

    public ImagePanelInputController getDefaultInputController();

}
