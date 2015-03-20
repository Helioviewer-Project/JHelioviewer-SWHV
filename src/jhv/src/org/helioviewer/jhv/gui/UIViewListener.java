package org.helioviewer.jhv.gui;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

/**
 * UI Listener to react on changes within a view.
 */

public interface UIViewListener {

    /**
     * Callback function that will be called on every change within a view or
     * its predecessors.
     * 
     * @param sender
     *            View which emitted the ChangeEvent
     * @param aEvent
     *            History of changes, containing the causes and views where
     *            changes took place.
     */
    public void UIviewChanged(View sender, ChangeEvent aEvent);

}
