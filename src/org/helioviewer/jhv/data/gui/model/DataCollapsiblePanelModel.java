package org.helioviewer.jhv.data.gui.model;

import java.util.HashSet;

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
