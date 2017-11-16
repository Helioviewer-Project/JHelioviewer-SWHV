package org.helioviewer.jhv.data.gui;

import java.util.HashSet;

import org.helioviewer.jhv.data.event.SWEKGroup;

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

}
