package org.helioviewer.jhv.events.gui;

import java.util.HashSet;

import org.helioviewer.jhv.events.SWEKGroup;

public class SWEKTreeModel {

    private static final HashSet<SWEKTreeModelListener> listeners = new HashSet<>();

    public static void addSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.add(swekTreeModelListener);
    }

    public static void removeSWEKTreeModelListener(SWEKTreeModelListener swekTreeModelListener) {
        listeners.remove(swekTreeModelListener);
    }

    public static void setStartLoading(SWEKGroup group) {
        listeners.forEach(listener -> listener.startedDownloadingGroup(group));
    }

    public static void setStopLoading(SWEKGroup group) {
        listeners.forEach(listener -> listener.stoppedDownloadingGroup(group));
    }

}
