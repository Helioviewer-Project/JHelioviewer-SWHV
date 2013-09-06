package org.helioviewer.viewmodelplugin.filter;

import java.util.AbstractList;
import java.util.LinkedList;

import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor.Type;

/**
 * Class represents a list which holds all {@link FilterTab}s of one image
 * layer.
 * 
 * @author Stephan Pagel
 */
public class FilterTabList {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private AbstractList<FilterTab> filterTabList = new LinkedList<FilterTab>();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Adds a tab to the list.
     * 
     * @param filterTab
     *            FilterTab which has to be added to the list.
     * @return true if the FilterTab could be added to the list; false
     *         otherwise.
     */
    public boolean add(FilterTab filterTab) {
        return filterTabList.add(filterTab);
    }

    /**
     * Returns number of FilterTabs controlled by the list.
     * 
     * @return Number of FilterTabs controlled by the list.
     */
    public int size() {
        return filterTabList.size();
    }

    /**
     * Returns the FilterTab at the specific position in this list.
     * 
     * @param index
     *            Index of element to return.
     * @return the FilterTab at the specific position in this list.
     */
    public FilterTab get(int index) {
        return filterTabList.get(index);
    }

    /**
     * Searches for the first found FilterTab which has the same type as the
     * given one.
     * 
     * @param type
     *            Type of FilterTab to search for.
     * @return the first found FilterTabPanelManager or null, if no
     *         FilterTabPanelManager of this type could be found.
     */
    public FilterTabPanelManager getFirstPanelManagerByType(FilterTabDescriptor.Type type) {

        for (FilterTab tab : filterTabList) {
            if (tab.getType() == type)
                return tab.getPaneManager();
        }

        return null;
    }

    /**
     * Searches for first found FilterTab which has the same title as the given
     * one.
     * 
     * @param title
     *            Title of FilterTab to search for.
     * @return the first found FilterTabPanelManager or null, if no FilterTab of
     *         this type could be found.
     */
    public FilterTabPanelManager getPanelManagerByTitle(String title) {

        for (FilterTab tab : filterTabList) {
            if (tab.getType() == Type.COSTUM && tab.getTitle().equals(title))
                return tab.getPaneManager();
        }

        return null;
    }
}
