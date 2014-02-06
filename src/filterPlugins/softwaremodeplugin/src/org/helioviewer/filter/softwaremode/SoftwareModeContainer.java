/**
 * 
 */
package org.helioviewer.filter.softwaremode;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer;

/**
 * Installs the dummy plugin.
 * 
 * @author Helge Dietert
 */
public class SoftwareModeContainer extends SimpleFilterContainer {
    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Container#getDescription()
     */
    public String getDescription() {
        return "Switches to software mode through filter";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getFilter()
     */
    @Override
    protected Filter getFilter() {
        return new SoftwareModeFilter();
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getFilterTab()
     */
    @Override
    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COSTUM, "Software mode");
    }

    /**
     * @see org.helioviewer.viewmodelplugin.interfaces.Container#getName()
     */
    public String getName() {
        return "Software mode filter";
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#getPanel()
     */
    @Override
    protected FilterPanel getPanel() {
        return new SoftwareModePanel();
    }

    /**
     * @see org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer#useFilter(org.helioviewer.viewmodel.view.FilterView)
     */
    @Override
    protected boolean useFilter(FilterView view) {
        return true;
    }

}
