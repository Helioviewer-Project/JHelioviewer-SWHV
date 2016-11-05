package org.helioviewer.jhv.data.guielements.model;

import java.util.HashSet;

import org.helioviewer.jhv.data.guielements.listeners.DataCollapsiblePanelModelListener;

public class DataCollapsiblePanelModel {

    private final HashSet<DataCollapsiblePanelModelListener> listeners = new HashSet<>();

    public void repackCollapsiblePanels() {
        for (DataCollapsiblePanelModelListener l : listeners) {
            l.repack();
        }
    }

    public void addListener(DataCollapsiblePanelModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataCollapsiblePanelModelListener listener) {
        listeners.remove(listener);
    }

}
