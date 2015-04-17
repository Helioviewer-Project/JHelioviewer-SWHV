package org.helioviewer.viewmodelplugin.filter;

/**
 * Class represents a tab in the GUI which contains filter controls of one
 * category.
 * 
 * @author Stephan Pagel
 */
public class FilterTab {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private FilterTabDescriptor descriptor;
    private FilterTabPanelManager paneManager;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param type
     *            Type of tab.
     * @param title
     *            Title of tab.
     * @param paneManager
     *            Pane manager
     */
    public FilterTab(FilterTabDescriptor.Type type, String title, FilterTabPanelManager paneManager) {
        this.descriptor = new FilterTabDescriptor(type, title);
        this.paneManager = paneManager;
    }

    /**
     * Returns type of tab.
     * 
     * @return Type of tab.
     */
    public FilterTabDescriptor.Type getType() {
        return this.descriptor.getType();
    }

    /**
     * Returns title of tab.
     * 
     * @return Title of tab.
     */
    public String getTitle() {
        return this.descriptor.getTitle();
    }

    /**
     * Returns tab area where to add filter control components.
     * 
     * @return Tab area where to add filter control components.
     */
    public FilterTabPanelManager getPaneManager() {
        return this.paneManager;
    }
}
