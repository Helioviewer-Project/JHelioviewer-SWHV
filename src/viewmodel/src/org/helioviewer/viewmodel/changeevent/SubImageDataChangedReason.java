package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the image data has changed.
 * 
 * @author Stephan Pagel
 * */
public class SubImageDataChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason
     */
    public SubImageDataChangedReason(View aView) {
        // memorize view
        view = aView;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

}
