package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;

/**
 * Abstract base class implementing ModifiableInnerViewView and ViewListener,
 * providing common functions for most views.
 * 
 * <p>
 * This class represents the least common denominator for most views. It manages
 * mostly everything related to view listeners and building the view chain.
 * 
 * @author Markus Langenberg
 * 
 */
public abstract class AbstractBasicView extends AbstractView implements ModifiableInnerViewView, ViewListener {

    protected View view;

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     * 
     * {@link #setViewSpecificImplementation(View, ChangeEvent)} is called
     * during this function.
     */
    public void setView(View newView) {
        if (view != null) {
            view.removeViewListener(this);
        }

        view = newView;

        ChangeEvent changeEvent = new ChangeEvent(new ViewChainChangedReason(this));

        setViewSpecificImplementation(newView, changeEvent);

        notifyViewListeners(changeEvent);

        if (view != null) {
            view.addViewListener(this);
        }
    }

    /**
     * Performs the view specific tasks necessary during setView.
     * 
     * This function is called from within setView and should contain only
     * implementation specific tasks such as precomputing adapters and
     * reapplying image operations.
     * 
     * @param newView
     *            The new direct successor of this view
     * @param changeEvent
     *            ChangeEvent that will be fired by setView. Additional reasons
     *            may be added.
     */
    protected abstract void setViewSpecificImplementation(View newView, ChangeEvent changeEvent);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this))
            return (T) this;
        else if (view != null)
            return view.getAdapter(c);
        else
            return null;
    }

}
