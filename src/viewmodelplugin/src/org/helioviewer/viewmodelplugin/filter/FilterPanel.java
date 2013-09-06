package org.helioviewer.viewmodelplugin.filter;

import javax.swing.JPanel;

import org.helioviewer.viewmodel.filter.Filter;

/**
 * Basic class for all visual filter controls. All control elements of a filter
 * have to be placed on this kind of a panel.
 * 
 * @author Stephan Pagel
 */
public abstract class FilterPanel extends JPanel {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    /**
     * Width of the title for components to display in adjust panel
     */
    public static final int titleWidth = 70;

    /**
     * Width of the value for components to display in adjust panel
     */
    public static final int valueWidth = 40;

    /**
     * Height for components to display in adjust panel
     */
    public static final int height = 20;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Sets the reference of the filter which has to be controlled by to this
     * panel.
     * 
     * @param filter
     *            Reference of the filter which has to be controlled by this
     *            component.
     */
    public abstract void setFilter(Filter filter);

    /**
     * Returns the position where to add the filter control component at the
     * panel of all filters.
     * 
     * @return position of filter control component.
     */
    public abstract FilterTabPanelManager.Area getArea();
}
