package org.helioviewer.jhv.gui.states;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;

/**
 * Singleton that controls the current state, i.e. 2D or 3D.
 * {@link StateChangeListener}s can be added for notifications about the current
 * state. By default, the 3D state is enabled.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class StateController {
    private final static StateController instance = new StateController();

    public static StateController getInstance() {
        return instance;
    }

    private final List<StateChangeListener> stateChangeListeners = new ArrayList<StateChangeListener>();

    private State currentState;

    private StateController() {
        set3DState();
    }

    public void set2DState() {
        setState(ViewStateEnum.View2D.getState());
        GL3DState.getActiveCamera().reset();
        Displayer.display();
    }

    public void set3DState() {
        setState(ViewStateEnum.View3D.getState());
    }

    private void setState(State newState) {
        State oldState = currentState;
        if (newState != oldState) {
            currentState = newState;
            fireStateChange(newState, oldState);
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public void addStateChangeListener(StateChangeListener listener) {
        this.stateChangeListeners.add(listener);
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    protected void fireStateChange(State newState, State oldState) {
        for (StateChangeListener listener : stateChangeListeners) {
            listener.stateChanged(newState, oldState, this);
        }
    }

    public static interface StateChangeListener {
        public void stateChanged(State newState, State oldState, StateController stateController);
    }

}
