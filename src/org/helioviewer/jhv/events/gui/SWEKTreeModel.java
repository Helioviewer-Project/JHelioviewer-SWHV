package org.helioviewer.jhv.events.gui;

import java.util.ArrayList;

import org.helioviewer.jhv.events.SWEKGroup;

public class SWEKTreeModel {

    private static final ArrayList<SWEKTreeModelListener> listeners = new ArrayList<>();

    static void addListener(SWEKTreeModelListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void setStartLoading(SWEKGroup group) {
        listeners.forEach(listener -> listener.startedDownloadingGroup(group));
    }

    public static void setStopLoading(SWEKGroup group) {
        listeners.forEach(listener -> listener.stoppedDownloadingGroup(group));
    }

}
