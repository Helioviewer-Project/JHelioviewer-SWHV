package org.helioviewer.jhv.gui;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

/**
 * This class distributes changes from the associated view chain to all
 * listeners which are part of the GUI.
 * 
 * Sometime components or parts of the GUI have to react on changes which
 * belongs to the view chain. The view listener distributer acts as a
 * registration point for these components.
 * <p>
 * It is guaranteed when something changed to the topmost view of the view chain
 * the view listener distributer will recognize it. There is no guaranteed for
 * that a component which registers it as view listener at a view itself
 * recognize changes to the view chain.
 * 
 * @author Markus Langenberg
 */
public class ViewListenerDistributor {

    private final static ViewListenerDistributor singletonObject = new ViewListenerDistributor();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ArrayList<ViewListener> listeners = new ArrayList<ViewListener>();

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     */
    public static ViewListenerDistributor getSingletonInstance() {
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
    public void addViewListener(ViewListener l) {
        rwl.writeLock().lock();
        listeners.add(l);
        rwl.writeLock().unlock();
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
    public void removeViewListener(ViewListener l) {
        rwl.writeLock().lock();
        listeners.remove(l);
        rwl.writeLock().unlock();
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
        rwl.readLock().lock();
        for (ViewListener listener : listeners) {
            listener.viewChanged(sender, aEvent);
        }
        rwl.readLock().unlock();
    }

}
