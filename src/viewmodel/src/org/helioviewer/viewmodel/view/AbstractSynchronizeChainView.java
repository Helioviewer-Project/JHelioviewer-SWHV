package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;

/**
 * Abstract base class implementing SynchronizeView, providing some common
 * functions.
 * 
 * <p>
 * A synchronize view can be registered (add to a view as listener) in other
 * view chains, too. If anything changes in another chain, the synchronize view
 * can react, depending on its specific implementation.
 * 
 * @author Stephan Pagel
 */
public abstract class AbstractSynchronizeChainView extends AbstractBasicView implements SynchronizeView {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // Container where references to all observed views are memorized
    protected View observedView;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * 
     * In this case, it does nothing.
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void setObservedView(View aView) {

        if (observedView != null) {
            observedView.removeViewListener(this);
        }

        // memorize observed view
        observedView = aView;

        // register in observed view as listener
        if (aView != null) {
            aView.addViewListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public View getObservedView() {
        return observedView;
    }

    /**
     * Analyzes a change reason from a view which was not caused by the own view
     * chain.
     * 
     * This function will be called for every change on every observed view.
     * 
     * @param sender
     *            Sender of the ChangeEvent in the observed view chain
     * @param aEvent
     *            ChangeEvent holding information about changes in the observed
     *            view chain
     */
    protected abstract void analyzeObservedView(View sender, ChangeEvent aEvent);

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This function will only be called from within the own view chain.
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (sender == view) {
            // sender is from own view chain, forward change reason
            notifyViewListeners(aEvent);
        } else {
            // sender is from other view chain, analyze change reason
            analyzeObservedView(sender, aEvent);
        }
    }
}