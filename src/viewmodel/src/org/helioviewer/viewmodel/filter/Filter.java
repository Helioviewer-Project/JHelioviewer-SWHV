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
     * Returns if the filter is a major filter or not. Major filter means that
     * it is a basic filter which will be supported in the whole program (e.g.
     * filter result will be visible in overview). If a filter is not a major
     * filter the program will not apply it in certain circumstances.
     * 
     * @return if it is a major filter
     * */
    public boolean isMajorFilter();

    /**
     * This method forces a refiltering of the image data the next time the
     * image is rendered regardless if the internal state of the filter.
     */
    public void forceRefilter();

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
}
