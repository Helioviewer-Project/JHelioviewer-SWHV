package org.helioviewer.viewmodel.view;

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
     */
    public void setView(View newView) {
        if (view != null) {
            view.removeViewListener(this);
        }

        view = newView;

        if (view != null) {
            view.addViewListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public final <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this))
            return (T) this;
        else if (view != null)
            return view.getAdapter(c);
        else
            return null;
    }

}
