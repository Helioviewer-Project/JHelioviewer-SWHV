package org.helioviewer.jhv.internal_plugins.filter.contrast;

import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer;

/**
 * Plugin for enhancing the contrast of the image.
 * 
 * <p>
 * The plugin manages a filter for enhancing the contrast and a slider to change
 * the parameter.
 * 
 * @author Markus Langenberg
 * 
 */
public class ContrastPlugin extends SimpleFilterContainer {

    /**
     * {@inheritDoc}
     */

    protected Filter getFilter() {
        return new ContrastFilter();
    }

    /**
     * {@inheritDoc}
     */

    protected boolean useFilter(FilterView view) {
        return true;
    }

    /**
     * {@inheritDoc}
     */

    protected FilterPanel getPanel() {
        return new ContrastPanel();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Contrast";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }
}
