package org.helioviewer.viewmodelplugin.filter;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor.Type;

/**
 * This basic class extends the {@link FilterContainer} by a default
 * implementation of the install process of a filter.
 * 
 * @author Stephan Pagel
 */
public abstract class SimpleFilterContainer extends FilterContainer {

    /**
     * {@inheritDoc}
     */

    protected void installFilterImpl(FilterView filterView, FilterTabList tabList) {

        // let filter check if it can be used at current position with current
        // data
        if (!useFilter(filterView))
            return;

        // add filter to filter view
        Filter filter = getFilter();
        filterView.setFilter(filter);

        // add the visual control part of the filter to GUI
        FilterTabDescriptor descriptor = getFilterTab();
        FilterTabPanelManager tabPaneManager;

        if (descriptor.getType() == Type.DEFAULT_FILTER || descriptor.getType() == Type.DEFAULT_MOVIE || descriptor.getType() == Type.COMPACT_FILTER)
            tabPaneManager = tabList.getFirstPanelManagerByType(descriptor.getType());
        else {
            tabPaneManager = tabList.getPanelManagerByTitle(descriptor.getTitle());
        }

        if (tabPaneManager == null) {
            tabPaneManager = new FilterTabPanelManager();
            tabList.add(new FilterTab(Type.COSTUM, descriptor.getTitle(), tabPaneManager));
        }

        FilterPanel pane = getPanel();

        if (pane != null) {
            pane.setFilter(filter);
            tabPaneManager.add(pane);
        }
    }

    /**
     * {@inheritDoc}
     */

    public Class<? extends Filter> getFilterClass() {
        return getFilter().getClass();
    }

    /**
     * Returns a boolean value if filter can be used at given position in view
     * chain with given data.
     * 
     * @param view
     *            predecessor in view chain.
     * @return true if the filter can be used in given environment and should be
     *         added to view chain; false otherwise.
     */
    protected abstract boolean useFilter(FilterView view);

    /**
     * Returns a new object of the contained filter.
     * 
     * @return A new object of the contained filter.
     */
    protected abstract Filter getFilter();

    /**
     * Returns a new control panel for the contained filter.
     * 
     * @return a new control panel for the contained filter.
     */
    protected abstract FilterPanel getPanel();

    /**
     * Specifies the position of the control panel of the filter inside the GUI.
     * 
     * @return Position of control panel inside the GUI.
     */
    protected abstract FilterTabDescriptor getFilterTab();
}
