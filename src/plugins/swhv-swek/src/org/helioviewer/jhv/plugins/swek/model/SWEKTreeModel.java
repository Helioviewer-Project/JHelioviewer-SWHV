package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;

/**
 * This model manages all the SWEKEventTypeTreeModels and delegate events. This
 * was primarily created to handle the selection in the distributed event trees.
 *
 * The SWEKTreeModel is the central point of access.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKTreeModel {
    /** The singleton instance of the SWEKTreeModel */
    private static SWEKTreeModel singletonInstance;

    /** Holder for the SWEK event type tree models */
    private final List<SWEKTreeModelListener> listeners;

    /**  */
    private final Map<SWEKEventType, Integer> loadingTypes;

    private SWEKTreeModel() {
        listeners = new ArrayList<SWEKTreeModelListener>();
        loadingTypes = new HashMap<SWEKEventType, Integer>();
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
        listeners.add(swekTreeModelListener);
    }

    /**
     * removes a SWEK tree model listener.
     *
     * @param swekTreeModelListener
     *            the listener to remove
     */
    public void removeSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.remove(swekTreeModelListener);
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
     * Sets the event type to start loading.
     *
     * @param eventType
     *            the event type that started loading
     */
    public void setStartLoading(SWEKEventType eventType) {
        if (loadingTypes.containsKey(eventType)) {
            Integer i = loadingTypes.get(eventType);
            i++;
            loadingTypes.put(eventType, i);
        } else {
            loadingTypes.put(eventType, 1);
            fireEventTypeStartLoading(eventType);
        }
    }

    /**
     * Sets the event type to stop loading.
     *
     * @param eventType
     *            the event type that stopped loading
     */
    public void setStopLoading(SWEKEventType eventType) {
        if (loadingTypes.containsKey(eventType)) {
            Integer i = loadingTypes.get(eventType);
            i--;
            if (i == 0) {
                loadingTypes.remove(eventType);
                fireEventTypeStopLoading(eventType);
            } else {
                loadingTypes.put(eventType, i);
            }
        } else {
            fireEventTypeStopLoading(eventType);
        }
    }

    /**
     * Inform the SWEK tree model listeners about a change of the tree.
     */
    private void fireTreeExpansionChanged() {
        for (SWEKTreeModelListener l : listeners) {
            l.expansionChanged();
        }
    }

    /**
     * Inform the tree model listeners an event type started loading.
     *
     * @param eventType
     *            the event type that started loading
     */
    private void fireEventTypeStartLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.startedDownloadingEventType(eventType);
        }
    }

    /**
     * Inform the tree model listeners an event type stopped loading.
     *
     * @param eventType
     *            the event type that stopped loading
     */
    private void fireEventTypeStopLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.stoppedDownloadingEventType(eventType);
        }

    }
}
