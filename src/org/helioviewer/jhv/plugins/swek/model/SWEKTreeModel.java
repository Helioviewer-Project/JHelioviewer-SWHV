package org.helioviewer.jhv.plugins.swek.model;

import java.util.HashSet;

import org.helioviewer.jhv.data.event.SWEKGroup;

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

    public static void setStartLoading(SWEKGroup group) {
        for (SWEKTreeModelListener l : listeners) {
            l.startedDownloadingGroup(group);
        }
    }

    public static void setStopLoading(SWEKGroup group) {
        for (SWEKTreeModelListener l : listeners) {
            l.stoppedDownloadingGroup(group);
        }
    }
/*
    public static void resetGroup(SWEKGroup group) {
        setStopLoading(group);
    }
*/
}
