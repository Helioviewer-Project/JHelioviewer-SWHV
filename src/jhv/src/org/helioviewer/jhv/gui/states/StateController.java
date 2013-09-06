package org.helioviewer.jhv.gui.states;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.opengl.GLInfo;

/**
 * Singleton that controls the current state, i.e. 2D or 3D.
 * {@link StateChangeListener}s can be added for notifications about the current
 * state. By default, the 3D state is enabled.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class StateController {
    private static StateController instance = new StateController();

    private List<StateChangeListener> stateChangeListeners = new ArrayList<StateChangeListener>();

    private State currentState;

    private StateController() {
        if (!GLInfo.glIsEnabled()) {
            // throw new
            // IllegalStateException("Cannot create GL3DViewchainFactory when OpenGL is not available!");
            set2DState();
        } else {
            set3DState();
        }
    }

    public static StateController getInstance() {
        return StateController.instance;
    }

    public void set2DState() {
        this.setState(ViewStateEnum.View2D.getState());
    }

    public void set3DState() {
        this.setState(ViewStateEnum.View3D.getState());
    }

    private void setState(State newState) {
        State oldState = this.currentState;
        if (newState != oldState) {
            this.currentState = newState;
            fireStateChange(newState, oldState);
        }
    }

    public State getCurrentState() {
        return this.currentState;
    }

    public void addStateChangeListener(StateChangeListener listener) {
        synchronized (this.stateChangeListeners) {
            this.stateChangeListeners.add(listener);
        }
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        synchronized (this.stateChangeListeners) {
            this.stateChangeListeners.remove(listener);
        }
    }

    protected void fireStateChange(State newState, State oldState) {
        synchronized (this.stateChangeListeners) {
            for (StateChangeListener listener : this.stateChangeListeners) {
                listener.stateChanged(newState, oldState, this);
            }
        }
    }

    public static interface StateChangeListener {
        public void stateChanged(State newState, State oldState, StateController stateController);
    }
}
