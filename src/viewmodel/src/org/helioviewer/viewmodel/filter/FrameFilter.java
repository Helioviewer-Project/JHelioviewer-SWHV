package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 * Filter which works over several frames.
 * <p>
 * Playing a movie it becomes interesting applying filter which works with the
 * previous image or a different fixed image for running differences as well.
 * <p>
 * The decoding is running frame by frame and through the interface a cache will
 * be established. It is also possible to query other frames, but this will
 * freeze the view chain and should therefore only be done very rarely (i.e.
 * ones a movie or when the user change a setting)
 *
 * @author Helge Dietert
 *
 */
public interface FrameFilter extends Filter {
    /**
     * Sets the time machine data for this filter.
     *
     * @param data
     *            Reference to access previous frames
     */
    public void setJPXView(JHVJPXView data);

}
