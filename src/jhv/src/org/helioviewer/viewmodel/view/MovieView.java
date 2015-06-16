package org.helioviewer.viewmodel.view;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

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
     * Returns the image cache status.
     *
     * @return image cache status
     */
    public ImageCacheStatus getImageCacheStatus();

    /**
     * Returns time stamp of any frame specified.
     * 
     * @param frame
     * @return time stamp of the requested frame
     */
    public ImmutableDateTime getFrameDateTime(int frame);

    // <!- only for Layers
    public void setFrame(int frame);
    public int getFrame(ImmutableDateTime time);
    // -->

}
