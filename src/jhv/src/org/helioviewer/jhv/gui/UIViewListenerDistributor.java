package org.helioviewer.jhv.gui;

import java.util.ArrayList;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;
//import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;

/**
 * This class distributes changes from the associated view chain to all
 * listeners which are part of the GUI.
 *
 * Sometime components or parts of the GUI have to react on changes which
 * belongs to the view chain. The view listener distributer acts as a
 * registration point for these components.
 *
 * @author Markus Langenberg
 */
public class UIViewListenerDistributor {

    private final static UIViewListenerDistributor singletonObject = new UIViewListenerDistributor();
    private final ArrayList<UIViewListener> listeners = new ArrayList<UIViewListener>();

    /**
     * Returns the only instance of this class.
     *
     * @return the only instance of this class.
     */
    public static UIViewListenerDistributor getSingletonInstance() {
        return singletonObject;
    }

    /**
     * Adds a view listener.
     *
     * This listener will be called on every change from views deeper in the
     * view chain.
     *
     * @param l
     *            the listener to add
     * @see #removeViewListener(ViewListener)
     */
    public void addViewListener(UIViewListener l) {
        listeners.add(l);
    }

    /**
     * Removes a view listener.
     *
     * The listener no longer will be informed about changes from views deeper
     * in the view chain.
     *
     * @param l
     *            the listener to remove
     * @see #addViewListener(ViewListener)
     */
    public void removeViewListener(UIViewListener l) {
        listeners.remove(l);
    }

    /**
     * {@inheritDoc}
     */
    /**
     * Informs all listeners of changes made to previous views.
     *
     * For the reason that this is just a distributor it forwards the events and
     * the sender.
     *
     * @param sender
     *            last view which sent the change event or forwarded it.
     * @param event
     *            event which contains the associated reasons.
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        for (UIViewListener listener : listeners) {
            listener.UIviewChanged(sender, aEvent);
        }

    }

}
