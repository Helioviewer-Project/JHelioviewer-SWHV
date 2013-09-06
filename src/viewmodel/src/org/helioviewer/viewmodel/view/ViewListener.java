package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;

/**
 * Listener to react on changes within a view.
 * 
 * <p>
 * This class provides the capability to catch events from within the view
 * chain. Therefore, the implementing class does not necessarily have to be a
 * view itself, other classes working together with the view chain without being
 * a view themselves also are able to be a view listener. On the other hand,
 * since implementing this interface is the only way to receives changes within
 * the viewchain, every view except from the undermost view should implement
 * this interface.
 * 
 * <p>
 * When a change takes place, a ChangeEvent is emitted, containing informations
 * about the cause and consequences of the change.
 * 
 * <p>
 * There are no guarantees about which thread viewChanged will be called from.
 * Since most of the Java GUI methods are not thread-safe (see
 * http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html), it is
 * important that ViewListeners either do not interact with the UI or make sure
 * that they are executed from the event thread. The interface GUIViewListener
 * together with the ViewHelper method ensureGUIViewListenerExecutionInGUIThread
 * were introduced in order to make this decision for ViewListeners that do not
 * need fine granularity control over which thread each part of their
 * viewChanged method is executed in.
 * 
 * <p>
 * Please see org.helioviewer.jhv.gui.components.QualitySlider for an example
 * implementation of GUIViewListener.
 * 
 * <p>
 * For further information about change event, see
 * {@link org.helioviewer.viewmodel.changeevent}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ViewListener {

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
    public void viewChanged(View sender, ChangeEvent aEvent);

}
