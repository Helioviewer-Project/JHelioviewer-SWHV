package org.helioviewer.viewmodel.filter;

/**
 * Interface common for all filters.
 *
 * <p>
 * A filter represents an image processing operation, such as point operations
 * and convolutions.
 *
 * <p>
 * To actually apply the filter to an image within the view chain, it has to be
 * passed to a {@link org.helioviewer.viewmodel.view.FilterView}. Every time a
 * new image passes the FilterView, it will call the filter.
 *
 * @author Ludwig Schmidt
 */
public interface Filter {

    /**
     * Sets the filter state.
     *
     * The format of the state is determined by the filter itself. It should
     * encode all necessary values to restore the filter.
     *
     * @param state
     *            The new filter state
     * @see #getState()
     */
    public void setState(String state);

    /**
     * Gets the filter state.
     *
     * The format of the state is determined by the filter itself. It should
     * encode all necessary values to restore the filter.
     *
     * @param state
     *            The new filter state
     * @see #setState()
     */
    public String getState();

    /**
     * Adds a filter listener.
     *
     * This listener will be called on every change within this filter, such as
     * changing parameters.
     *
     * @param l
     *            the listener to add
     * @see #removeFilterListener(FilterListener)
     */
    public void addFilterListener(FilterListener l);

    /**
     * Removes a filter listener.
     *
     * This listener will no longer be called on every change within this
     * filter, such as changing parameters.
     *
     * @param l
     *            the listener to add
     * @see #addFilterListener(FilterListener)
     */
    public void removeFilterListener(FilterListener l);
}
