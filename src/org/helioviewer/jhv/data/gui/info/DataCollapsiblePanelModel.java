package org.helioviewer.jhv.data.gui.info;

import java.util.HashSet;

class DataCollapsiblePanelModel {

    private final HashSet<DataCollapsiblePanelModelListener> listeners = new HashSet<>();

    void repackCollapsiblePanels() {
        for (DataCollapsiblePanelModelListener l : listeners) {
            l.repack();
        }
    }

    void addListener(DataCollapsiblePanelModelListener listener) {
        listeners.add(listener);
    }

    void removeListener(DataCollapsiblePanelModelListener listener) {
        listeners.remove(listener);
    }

}
