package org.helioviewer.viewmodel.filter;

import java.util.LinkedList;

public abstract class AbstractFilter implements ObservableFilter {

    private LinkedList<FilterListener> listeners = new LinkedList<FilterListener>();

    /**
     * {@inheritDoc}
     */
    public void addFilterListener(FilterListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeFilterListener(FilterListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * Notifies all registered listeners, that something has changed.
     */
    protected void notifyAllListeners() {
        synchronized (listeners) {
            for (FilterListener f : listeners) {
                f.filterChanged(this);
            }
        }
    }
}
