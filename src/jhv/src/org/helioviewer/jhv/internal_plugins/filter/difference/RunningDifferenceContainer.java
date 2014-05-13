package org.helioviewer.jhv.internal_plugins.filter.difference;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer;

/**
 * Container to install for the plugin
 * 
 * @author Helge Dietert
 * 
 */
public class RunningDifferenceContainer extends SimpleFilterContainer {

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Container#getDescription()
     */
    public String getDescription() {
        return "Enables running difference on movies";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getFilter()
     */
    @Override
    protected Filter getFilter() {
        return new RunningDifferenceFilter();
    }

    /**
     * Sets the position for the plugin in the gui TODO What type?
     * 
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getFilterTab()
     */
    @Override
    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.DEFAULT_MOVIE, "Movie");
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Container#getName()
     */
    public String getName() {
        return "Running Difference";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getPanel()
     */
    @Override
    protected FilterPanel getPanel() {
        return new RunningDifferencePanel();
    }

    /**
     * It makes only sense for movies
     * 
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#useFilter(org.helioviewer.viewmodel.view.FilterView)
     */
    @Override
    protected boolean useFilter(FilterView view) {
        return view.getAdapter(MovieView.class) != null;
    }
}
