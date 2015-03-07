package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Class represents a change reason when the viewport has changed.
 * 
 * @author Stephan Pagel
 * */
public class ViewportChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;

    // memorizes the viewport
    private Viewport viewport;

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aViewport
     *            The new viewport.
     * */
    public ViewportChangedReason(View aView, Viewport aViewport) {
        // memorize parameter values
        view = aView;
        viewport = aViewport;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new viewport which has changed when this change reason
     * occurred.
     * 
     * @return new viewport.
     * */
    public Viewport getViewport() {
        return viewport;
    }

}
