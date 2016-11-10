package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;

/**
 * This model manages all the SWEKEventTypeTreeModels and delegate events. This
 * was primarily created to handle the selection in the distributed event trees.
 *
 * The SWEKTreeModel is the central point of access.
 */
public class SWEKTreeModel {

    private static final HashSet<SWEKTreeModelListener> listeners = new HashSet<>();

    public static void addSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.add(swekTreeModelListener);
    }

    public static void removeSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.remove(swekTreeModelListener);
    }

    public static void subTreeCollapsed() {
        fireTreeExpansionChanged();
    }

    public static void subTreeExpanded() {
        fireTreeExpansionChanged();
    }

    public static void setStartLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.startedDownloadingEventType(eventType);
        }
    }

    public static void setStopLoading(SWEKEventType eventType) {
        for (SWEKTreeModelListener l : listeners) {
            l.stoppedDownloadingEventType(eventType);
        }
    }

    public static void resetEventType(SWEKEventType eventType) {
        setStopLoading(eventType);
    }

    private static void fireTreeExpansionChanged() {
        for (SWEKTreeModelListener l : listeners) {
            l.expansionChanged();
        }
    }

}
