package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * View to interact with time stamp providing image series.
 * 
 * <p>
 * This interface is designed to interact with time stamp providing image
 * series. Besides from basic functions like set/getCurrentFrame, there is one
 * additional mechanism, that should be explained: Image series with time stamps
 * provide two basic modes, how to play an movie: The show every frame,
 * independent from the different time stamps of each frame or the speed of the
 * computer. This mode will be called "relative mode" or "simple mode". The
 * other one, called "absolute mode" or "physical mode", shows the frames in a
 * way, that the physical time you can see stays constant on every machine. This
 * affects two aspects: If the desired speed is too high for the computer, the
 * TimedMovieView should automatically skip frames to keep its speed. On the
 * other hand, if the time gaps between frames vary, the TimedMovieView should
 * adjust its frame rate as well.
 * 
 * <p>
 * Apart from that, timed movie views can be linked together, so that they will
 * be animated simultaneously. This means, that all frames will stay as close
 * together as possible. For further information about linked movies, also see
 * {@link LinkedMovieManager}.
 * 
 * @author Markus Langenberg
 */
public interface TimedMovieView extends MovieView {

    /**
     * Sets the frame currently shown.
     * 
     * Searches the closest frame to the given time.
     * 
     * @param time
     *            time which should be matches as close as possible
     * @param event
     *            ChangeEvent to append new ChangeReasons
     * @see MovieView#setCurrentFrame
     * @see #getCurrentFrameNumber
     * @see #getMaximumFrameNumber
     */
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event);

    /**
     * Sets the frame currently shown.
     * 
     * Searches the closest frame to the given time.
     * 
     * @param time
     *            time which should be matches as close as possible
     * @param event
     *            ChangeEvent to append new ChangeReasons
     * @param forceSignal
     *            Forces a reader signal and depending on the reader mode a
     *            render signal regardless whether the frame changed
     * @see MovieView#setCurrentFrame
     * @see #getCurrentFrameNumber
     * @see #getMaximumFrameNumber
     */
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event, boolean forceSignal);

    /**
     * Returns time stamp of the current frame.
     * 
     * @return time stamp of the current frame
     */
    public ImmutableDateTime getCurrentFrameDateTime();

    /**
     * Returns time stamp of any frame specified.
     * 
     * @param frameNumber
     * @return time stamp of the requested frame
     */
    public ImmutableDateTime getFrameDateTime(int frameNumber);

    /**
     * Sets the desired absolute speed.
     * 
     * Implicit, switches to absolute mode and thus overrides all settings
     * previously set by {@link MovieView#setDesiredRelativeSpeed}
     * 
     * @param observationSecondsPerSecond
     *            desired absolute speed
     * @see MovieView#setDesiredRelativeSpeed
     */
    public void setDesiredAbsoluteSpeed(int observationSecondsPerSecond);

    /**
     * Puts the movie view to the set of movies, which are playing
     * simultaneously.
     * 
     * @see #unlinkMovie()
     */
    public void linkMovie();

    /**
     * Removes the movie view from the set of movies, which are playing
     * simultaneously.
     * 
     * @see #linkMovie()
     */
    public void unlinkMovie();

    public LinkedMovieManager getLinkedMovieManager();

}
