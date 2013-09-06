package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.filter.Filter;

/**
 * View for applying any kind of filter.
 * 
 * <p>
 * A FilterView provides functions to apply a Filter to the current Image, such
 * as sharpening, gamma correction or any other kind of image processing
 * operation.
 * 
 * <p>
 * It should provide all the information, the filter needs to the filter. To
 * request this information, the filter can to implement different interfaces.
 * To get further informations about how to use filters, see
 * {@link org.helioviewer.viewmodel.filter}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface FilterView extends ModifiableInnerViewView, ViewListener {

    /**
     * Returns the filter currently used by the view
     * 
     * @return filter in use, null if there is none
     * @see #setFilter
     */
    public Filter getFilter();

    /**
     * Attaches a new filter to the FilterView.
     * 
     * If there is already a filter attached to the view, the old filter will be
     * removed.
     * 
     * @param f
     *            the new filter
     * @see #getFilter
     */
    public void setFilter(Filter f);
}
