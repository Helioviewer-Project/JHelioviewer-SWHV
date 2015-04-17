package org.helioviewer.viewmodelplugin.filter;

/**
 * Class which identifies a FilterTab.
 * 
 * @author Stephan Pagel
 */
public class FilterTabDescriptor {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    public enum Type {
        DEFAULT_FILTER, DEFAULT_MOVIE, COSTUM, COMPACT_FILTER
    }

    private Type type;
    private String title;

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
     */
    public FilterTabDescriptor(Type type, String title) {
        this.type = type;
        this.title = title;
    }

    /**
     * Returns type of tab.
     * 
     * @return Type of tab.
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns title of tab.
     * 
     * @return Title of tab.
     */
    public String getTitle() {
        return this.title;
    }
}
