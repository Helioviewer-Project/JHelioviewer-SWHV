package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.ArrayList;

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

    protected final AbstractList<ViewListener> listeners = new ArrayList<ViewListener>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addViewListener(ViewListener l) {
        listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractList<ViewListener> getAllViewListeners() {
        AbstractList<ViewListener> listenersCopy = new ArrayList<ViewListener>(listeners);
        return listenersCopy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeViewListener(ViewListener l) {
        listeners.remove(l);
    }

    /**
     * Sends a new ChangeEvent to all registered view listeners.
     *
     * @param aEvent
     *            ChangeEvent to send
     */
    protected void notifyViewListeners(ChangeEvent aEvent) {
        for (ViewListener v : listeners) {
            v.viewChanged(this, aEvent);
        }
    }

}
