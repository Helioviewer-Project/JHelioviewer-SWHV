package org.helioviewer.jhv.gui.states;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.opengl.GLInfo;

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
        if (!GLInfo.glIsEnabled()) {
            // throw new
            // IllegalStateException("Cannot create GL3DViewchainFactory when OpenGL is not available!");
            set2DState();
        } else {
            set3DState();
        }
    }

    public void set2DState() {
        setState(ViewStateEnum.View2D.getState());
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
        synchronized (stateChangeListeners) {
            this.stateChangeListeners.add(listener);
        }
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        synchronized (stateChangeListeners) {
            stateChangeListeners.remove(listener);
        }
    }

    protected void fireStateChange(State newState, State oldState) {
        synchronized (stateChangeListeners) {
            for (StateChangeListener listener : stateChangeListeners) {
                listener.stateChanged(newState, oldState, this);
            }
        }
    }

    public static interface StateChangeListener {
        public void stateChanged(State newState, State oldState, StateController stateController);
    }
}
