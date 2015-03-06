package org.helioviewer.viewmodel.filter;

import java.util.ArrayList;

public abstract class AbstractFilter implements Filter {

    private final ArrayList<FilterListener> listeners = new ArrayList<FilterListener>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFilterListener(FilterListener l) {
        listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFilterListener(FilterListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all registered listeners, that something has changed.
     */
    protected void notifyAllListeners() {
        for (FilterListener f : listeners) {
            f.filterChanged(this);
        }
    }

}
