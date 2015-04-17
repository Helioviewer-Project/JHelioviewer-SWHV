package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

/**
 * View to interact with image series.
 *
 * <p>
 * This interface is designed to interact with image series, such as JPX-Files.
 * It provides basic functions to navigate within the image series.
 *
 * <p>
 * Additional features are provides by {@link TimedMovieView}.
 *
 * @author Markus Langenberg
 */
public interface MovieView extends View {

    /**
     * Animation mode.
     *
     * @see MovieView#setAnimationMode(AnimationMode)
     */
    public enum AnimationMode {
        LOOP {
            @Override
            public String toString() {
                return "Loop";
            }
        },
        STOP {
            @Override
            public String toString() {
                return "Stop";
            }
        },
        SWING {
            @Override
            public String toString() {
                return "Swing";
            }
        }
    }

    /**
     * Sets the frame currently shown.
     *
     * The given frameNumber is cropped to [0, maximumFrameNumber).
     *
     * @param frameNumber
     *            number of new frame, first frame = 0
     * @param event
     *            ChangeEvent to append new ChangeReasons
     * @see #getCurrentFrameNumber
     * @see #getMaximumFrameNumber
     * @see TimedMovieView#setCurrentFrame
     */
    public void setCurrentFrame(int frameNumber);

    /**
     * Sets the frame currently shown.
     *
     * The given frameNumber is cropped to [0, maximumFrameNumber).
     *
     * @param frameNumber
     *            number of new frame, first frame = 0
     * @param event
     *            ChangeEvent to append new ChangeReasons
     * @param forceSignal
     *            Forces a reader signal and depending on the reader mode a
     *            render signal regardless whether the frame changed
     * @see #getCurrentFrameNumber
     * @see #getMaximumFrameNumber
     * @see TimedMovieView#setCurrentFrame
     */
    public void setCurrentFrame(int frameNumber, boolean forceSignal);

    /**
     * Returns the current frame number.
     *
     * @return current frame number
     * @see #setCurrentFrame
     * @see TimedMovieView#setCurrentFrame
     */
    public int getCurrentFrameNumber();

    /**
     * Returns the maximum frame number.
     *
     * @return maximum frame number
     */
    public int getMaximumFrameNumber();

    /**
     * Returns the last accessible frame number.
     *
     * @return maximum accessible frame number
     */
    public int getMaximumAccessibleFrameNumber();

    /**
     * Sets the current animation mode.
     *
     * The animation mode describes, what should happen when the last frames is
     * reached:
     * <p>
     * LOOP: Go back to first frame and start again.<br>
     * STOP: Go back to first frame and stop playing.<br>
     * SWING: On reaching the last frame, play movie backwards, on reaching the
     * first frame again, play movie forwards.
     *
     * @param mode
     *            new animation mode
     */
    public void setAnimationMode(AnimationMode mode);

    /**
     * Sets the desired frame rate.
     *
     * Implicit, switches to relative mode and thus overrides all settings
     * previously set by {@link TimedMovieView#setDesiredAbsoluteSpeed}.
     *
     * @param framesPerSecond
     *            desired frame rate
     * @see TimedMovieView#setDesiredAbsoluteSpeed
     */
    public void setDesiredRelativeSpeed(int framesPerSecond);

    /**
     * Returns the frame rate, on which the MovieView is operating right now.
     *
     * The number has not be recalculated every frame, so changes on the desired
     * frame rate may not be visible immediately.
     *
     * @return average actual frame rate
     */
    public float getActualFramerate();

    /**
     * Starts playing the movie at the current frame.
     *
     * @see #pauseMovie
     * @see #isMoviePlaying
     */
    public void playMovie();

    /**
     * Pauses playing the movie at the current frame.
     *
     * @see #playMovie
     * @see #isMoviePlaying
     */
    public void pauseMovie();

    /**
     * Returns whether the movie is playing right now
     *
     * @return true if movie is playing, false otherwise
     * @see #playMovie
     * @see #pauseMovie
     */
    public boolean isMoviePlaying();

    /**
     * Sets whether the byte and integer buffers should be reused between
     * frames.
     * <p>
     * Normally this avoids garbage collection, but if you want to save the
     * previous frame or similar this cause problems. By default this is true
     * and views needing new buffers can request it
     *
     * @param reuseBuffer
     *            New boolean whether to reuse buffer
     */
    public void setReuseBuffer(boolean reuseBuffer);

    /**
     * Sets whether the buffers shall be reused between frames.
     *
     * @return The current behaviour to reuse buffer
     * @see #setReuseBuffer(boolean)
     */
    public boolean isReuseBuffer();

    public long getCurrentDateMillis();

    public int getDesiredRelativeSpeed();

   /**
     * Returns the image cache status.
     *
     * @return image cache status
     */
    public ImageCacheStatus getImageCacheStatus();

}
