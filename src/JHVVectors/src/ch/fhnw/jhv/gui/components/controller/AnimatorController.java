package ch.fhnw.jhv.gui.components.controller;

import java.util.ArrayList;

import ch.fhnw.jhv.gui.components.MoviePanel;
import ch.fhnw.jhv.gui.viewport.ViewPort;

/**
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         19.08.2011
 */
public class AnimatorController {

    /**
     * Animation Action
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         19.08.2011
     */
    public enum AnimationAction {
        PLAY, PAUSE, STOP, GOTOFRAME;
    }

    /**
     * Different Animation Modes Currently not supported by the User Interface
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         19.08.2011
     */
    public enum AnimationMode {
        LOOP {

            public String toString() {
                return "Loop";
            }
        },
        STOP {

            public String toString() {
                return "Stop";
            }
        },
        SWING {

            public String toString() {
                return "Swing";
            }
        }
    }

    /**
     * A class that provides information about an Animation Event. For Example
     * if Pause or Play was clicked in the MoviePanel
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         19.08.2011
     */
    public class AnimationEvent {
        private float timeStamp;
        private boolean isInterpolated;
        private float timeDeltaPerTick;
        private float timeDeltaPerFrame;
        private AnimationAction action;

        public AnimationEvent(float time, float timePerFrame, boolean isInterpolated, AnimationAction action) {
            this.timeStamp = time;
            this.timeDeltaPerTick = 1 / ((float) ViewPort.FPS) / timePerFrame;
            this.timeDeltaPerFrame = timePerFrame;
            this.isInterpolated = isInterpolated;
            this.action = action;
        }

        /**
         * get current frame number
         * 
         * @return
         */
        public float getFrameNumber() {
            return timeStamp;
        }

        /**
         * is interpolated on
         * 
         * @return
         */
        public boolean isInterpolated() {
            return isInterpolated;
        }

        /**
         * get the time delta per frame renderer
         * 
         * @return
         */
        public float getTimeDeltaPerTick() {
            return timeDeltaPerTick;
        }

        /**
         * get the time delta for one frame
         * 
         * @return
         */
        public float getTimeDeltaPerFrame() {
            return timeDeltaPerFrame;
        }

        /**
         * get the AnimationAction that occured with this event
         * 
         * @return
         */
        public AnimationAction getAction() {
            return action;
        }

    }

    /**
     * Interface to receive Events from Animator Controller
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         19.08.2011
     */
    public interface Listener {

        public void animationAction(AnimationEvent event);

    }

    /**
     * List of listeners
     */
    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    /**
     * how many seconds passed between a render call
     */
    private float interpolationTime;

    /**
     * how many seconds passed between rendering two images
     */
    private float deltaTimePerTick;

    /**
     * the current time in the movie
     */
    private float currentTimestamp = 0f;

    /**
     * the maximum time
     */
    private int timeDimensionCount = 0;

    /**
     * true if interpolated is on in the moviepanel
     */
    private boolean isInterpolated = false;

    /**
     * Animation mode of video play back
     */
    private AnimationMode animationMode = AnimationMode.STOP;

    /**
     * instance of the movie panel
     */
    private MoviePanel moviepanel;

    /**
     * if true, the timestamp must be increased on every call to tick
     */
    private boolean isPlaying = false;

    /**
     * Constructor
     */
    public AnimatorController() {
        setInterpolationTime(2.0f);
    }

    /**
     * Set the movie panel
     * 
     * @param panel
     *            Movie Panel of JHVVecotrs
     */
    public void setMoviePanel(MoviePanel panel) {
        this.moviepanel = panel;
    }

    /**
     * Notify all Listeners
     * 
     * @param action
     *            Action of Event
     */
    private void notifyListeners(AnimationAction action) {
        if (listeners.size() > 0) {
            AnimationEvent eventData = new AnimationEvent(currentTimestamp, interpolationTime, isInterpolated, action);
            for (Listener listener : listeners) {
                listener.animationAction(eventData);
            }
        }
    }

    /**
     * specify if the user clicked play or pause
     * 
     * @param isPlaying
     *            if true, the timstamp will be increased from now on
     */
    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;

        notifyListeners(isPlaying ? AnimationAction.PLAY : AnimationAction.PAUSE);
    }

    /**
     * set interpolation time
     * 
     * @param interpolationTime
     *            time between two frames
     */
    public void setInterpolationTime(float interpolationTime) {
        // TODO maybe FPS can be passed to AnimatorController
        // since it doesnt change at runtime
        this.deltaTimePerTick = 1 / ((float) ViewPort.FPS) / interpolationTime;
        this.interpolationTime = interpolationTime;
    }

    /**
     * Add a Listener
     * 
     * @param listener
     *            New Listener
     */
    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove Listener
     * 
     * @param listener
     *            Listener to be removed
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Update the movie panel with the current timestamp
     */
    private void updateMoviePanel() {
        moviepanel.updateMoviePanel((int) currentTimestamp);
    }

    /**
     * turn interpolated on
     * 
     * @param isInterpolated
     */
    public void setInterpolatd(boolean isInterpolated) {
        this.isInterpolated = isInterpolated;
    }

    /**
     * Change AnimationMode
     * 
     * @param mode
     *            new AnimationMode
     */
    public void setAnimationMode(AnimationMode mode) {
        this.animationMode = mode;
    }

    /**
     * Set the maximum count of time dimensions. The Animator will animate until
     * the current timestamp is less. greater than the max amount of time
     * dimensions.
     * 
     * @param dimensions
     */
    public void setTimeDimensionCount(int dimensions) {
        if (dimensions <= 1) {
            moviepanel.setEnabled(false);
            currentTimestamp = 0;
        } else {
            currentTimestamp = 0;
            timeDimensionCount = dimensions;
            moviepanel.setEnabled(true);
            moviepanel.init(dimensions);
            updateMoviePanel();
        }
    }

    /**
     * This method must be called on every render cycle that the current time
     * stamp can correctly be managed
     */
    public void tick() {
        if (isPlaying) {
            if (currentTimestamp + deltaTimePerTick >= (timeDimensionCount - 1)) {
                switch (animationMode) {
                case LOOP:
                    if (currentTimestamp + deltaTimePerTick > timeDimensionCount) {
                        jumpToFrameNumber(0);
                    } else {
                        currentTimestamp += deltaTimePerTick;
                    }
                    break;
                case STOP:
                    isPlaying = false;
                    moviepanel.setPlaying(false);
                    currentTimestamp += deltaTimePerTick;
                    // make sure the animation goes to the last frame
                    notifyListeners(AnimationAction.GOTOFRAME);
                    // make sure all listeneres know the video has stopped
                    notifyListeners(AnimationAction.STOP);
                    updateMoviePanel();
                    break;
				default:
					break;
                }
            } else {
                currentTimestamp += deltaTimePerTick;
                updateMoviePanel();
            }
        }
    }

    /**
     * go to next frame
     */
    public void nextFrame() {
        jumpToFrameNumber((int) currentTimestamp + 1);
    }

    /**
     * go to previous frame
     */
    public void previousFrame() {
        jumpToFrameNumber((int) currentTimestamp - 1);
    }

    /**
     * jump to a certain frame
     * 
     * @param frame
     *            number of target frame
     */
    public void jumpToFrameNumber(int frame) {
        if (frame >= 0 && frame < timeDimensionCount) {
            currentTimestamp = frame;
            updateMoviePanel();
            notifyListeners(AnimationAction.GOTOFRAME);
        }
    }

    /**
     * return the current timestamp
     * 
     * @return
     */
    public float getCurrentTimestamp() {
        return currentTimestamp;
    }

}
