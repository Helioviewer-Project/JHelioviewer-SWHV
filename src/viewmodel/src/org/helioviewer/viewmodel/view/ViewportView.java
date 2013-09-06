package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * View to manage the viewport of the image currently shown.
 * 
 * <p>
 * This view manages the viewport, which is currently displayed. By accessing
 * this view, changing the viewport is also possible.
 * 
 * <p>
 * Since the viewport represents a section of the screen or a comparable medium
 * (e.g. an output image), the size of the viewport is always given in pisels.
 * 
 * <p>
 * Note, that it is expected to have at least one ViewportView in every path of
 * the view chain. To take care of this requirement, implement the
 * {@link ImageInfoView} as recommended.
 * 
 * <p>
 * For further informations about viewports, also see
 * {@link org.helioviewer.viewmodel.viewport}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ViewportView extends View {

    /**
     * Sets the current viewport.
     * 
     * @param v
     *            The new viewport
     * @param event
     *            ChangeEvent to append all changes following
     * @return True, if the viewport has changed, false otherwise
     * @see #getViewport
     */
    public boolean setViewport(Viewport v, ChangeEvent event);

    /**
     * Returns the current viewport.
     * 
     * @return Current viewport
     * @see #setViewport
     */
    public Viewport getViewport();

}
