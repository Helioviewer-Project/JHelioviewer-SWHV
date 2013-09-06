package org.helioviewer.viewmodel.filter;

/**
 * Filter which is able to inform filter listeners about changes.
 * 
 * <p>
 * This filter will inform its listeners about changes within the filter, such
 * as changed filter parameters.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ObservableFilter extends Filter {

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
