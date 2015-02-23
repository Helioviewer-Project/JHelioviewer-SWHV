package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;

/**
 * Abstract base class implementing View, managing view listeners.
 * 
 * <p>
 * This class provides the functionality to manage and notify all view
 * listeners.
 * 
 * <p>
 * For further information about views, see {@link View}.
 * 
 * @author Markus Langenberg
 */
public abstract class AbstractView implements View {

    private final AbstractList<ViewListener> listeners = new ArrayList<ViewListener>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * {@inheritDoc}
     */
    public void addViewListener(ViewListener l) {
        rwl.writeLock().lock();
        listeners.add(l);
        rwl.writeLock().unlock();
    }

    /**
     * {@inheritDoc}
     */
    public AbstractList<ViewListener> getAllViewListener() {
        return listeners;
    }

    /**
     * {@inheritDoc}
     */
    public void removeViewListener(ViewListener l) {
        rwl.writeLock().lock();
        listeners.remove(l);
        rwl.writeLock().unlock();
    }

    /**
     * Sends a new ChangeEvent to all registered view listeners.
     * 
     * @param aEvent
     *            ChangeEvent to send
     */
    protected void notifyViewListeners(ChangeEvent aEvent) {
        rwl.readLock().lock();
        for (ViewListener v : listeners) {
            v.viewChanged(this, aEvent);
        }
        rwl.readLock().unlock();
    }

}
