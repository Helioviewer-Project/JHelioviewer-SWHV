package org.helioviewer.viewmodelplugin.filter;

import javax.swing.JPanel;

/**
 * Basic class for all visual filter controls. All control elements of a filter
 * have to be placed on this kind of a panel.
 * 
 * @author Stephan Pagel
 */
public abstract class FilterPanel extends JPanel {

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

    /**
     * Returns the position where to add the filter control component at the
     * panel of all filters.
     * 
     * @return position of filter control component.
     */
    public abstract FilterTabPanelManager.Area getArea();

}
