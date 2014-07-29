package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This model manages all the SWEKEventTypeTreeModels and delegate events. This
 * was primarily created to handle the selection in the distributed event trees.
 * 
 * The SWEKTreeModel is the central point of acces.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKTreeModel {
    /** The singleton instance of the SWEKTreeModel */
    private static SWEKTreeModel singletonInstance;

    /** Holder for the SWEK event type tree models */
    private final List<SWEKTreeModelListener> listeners;

    private SWEKTreeModel() {
        this.listeners = new ArrayList<SWEKTreeModelListener>();
    }

    public static SWEKTreeModel getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKTreeModel();
        }
        return singletonInstance;
    }

    /**
     * Adds a new SWEK tree model listener.
     * 
     * @param swekTreeModelListener
     *            the listener to add
     */
    public void addSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        this.listeners.add(swekTreeModelListener);
    }

    /**
     * removes a SWEK tree model listener.
     * 
     * @param swekTreeModelListener
     *            the listener to remove
     */
    public void removeSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        this.listeners.remove(swekTreeModelListener);
    }

    /**
     * Inform the SWEK tree model about a subtree that was collapsed.
     */
    public void subTreeCollapsed() {
        fireTreeExpansionChanged();
    }

    /**
     * Inform the SWEK tree model about a subtree that was expanded.
     */
    public void subTreeExpanded() {
        fireTreeExpansionChanged();
    }

    /**
     * Inform the SWEK tree model listeners about a change of the tree.
     */
    private void fireTreeExpansionChanged() {
        for (SWEKTreeModelListener l : this.listeners) {
            l.expansionChanged();
        }
    }
}
