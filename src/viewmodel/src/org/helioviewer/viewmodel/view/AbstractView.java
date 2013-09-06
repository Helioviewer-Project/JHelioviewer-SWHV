package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
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

    private AbstractList<ViewListener> listeners = new LinkedList<ViewListener>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * {@inheritDoc}
     */
    public void addViewListener(ViewListener l) {
        lock.writeLock().lock();
        listeners.add(l);
        lock.writeLock().unlock();
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
        lock.writeLock().lock();
        listeners.remove(l);
        lock.writeLock().unlock();
    }

    /**
     * Sends a new ChangeEvent to all registered view listeners.
     * 
     * @param aEvent
     *            ChangeEvent to send
     */
    protected void notifyViewListeners(ChangeEvent aEvent) {
        lock.readLock().lock();
        List<ViewListener> listenersCopy = null;
        try {
            listenersCopy = new ArrayList<ViewListener>(listeners);
        } finally {
            lock.readLock().unlock();
        }
        if (listenersCopy != null) {
            for (ViewListener v : listenersCopy) {
                v.viewChanged(this, aEvent);
            }
        }
    }

}
