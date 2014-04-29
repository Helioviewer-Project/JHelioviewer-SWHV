package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.imagedata.ImageData;

/**
 * Interface implemented by a {@link TimeMachineView} to get the data of other
 * (normally previous frames).
 * <p>
 * It is crucial to set the cache properly and only these data should be
 * accessed regularly.
 * 
 * @see TimeMachineView
 * @author Helge Dietert
 */
public interface TimeMachineData {
    /**
     * Gives an absolute frame in the movie.
     * <p>
     * This can trigger a different encoding and therefore should only be used
     * rarely to support a difference movie against a constant frame. There is
     * no guarantee that this results are cached even though the rest is.
     * 
     * @param pos
     *            Frame number
     * @return ImageData of the frame. Null if this frame if not available
     */
    public ImageData getAbsoluteFrame(int pos);

    /**
     * Gives back the image data of the frame before.
     * <p>
     * The relative position can be negative to get coming frames which will
     * trigger an extra encoding. But this may be necessary to show a sensible
     * first frame.
     * <p>
     * To ensure a proper updated cache, using classes must first require once
     * the normal ImageData and then acquire additional frames. Failing in this
     * may give wrong results! If using a normal filter, the StandardFilterView
     * already did this.
     * 
     * @param pos
     *            Number of frames to go back
     * @return ImageData of the frame pos times before. Null if this frame if
     *         not available
     */
    public ImageData getPreviousFrame(int pos);

    /**
     * Sets the number of previous frames we are interested in.
     * <p>
     * Since the cache may need to be updated when the region changes etc.
     * having a big size also can cost CPU time.
     * 
     * @param n
     *            Number of previous frames interested. 0 means the current
     *            frame and 1 the frame before
     */
    public void setPreviousCache(int n);

    public long getCurrentDateMillis();
}
