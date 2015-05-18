package org.helioviewer.jhv.data.guielements.model;

import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.data.guielements.listeners.DataCollapsiblePanelModelListener;

public class DataCollapsiblePanelModel {

    private final Set<DataCollapsiblePanelModelListener> listeners;

    public DataCollapsiblePanelModel() {
        listeners = new HashSet<DataCollapsiblePanelModelListener>();
    }

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
