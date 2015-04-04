package org.helioviewer.viewmodel.view;

import javax.swing.event.ChangeEvent;

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
    @Override
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View newView) {
        if (view != null) {
            view.removeViewListener(this);
        }

        view = newView;

        if (view != null) {
            view.addViewListener(this);
        }
    }

    protected abstract void setViewSpecificImplementation(View newView, ChangeEvent changeEvent);

}
