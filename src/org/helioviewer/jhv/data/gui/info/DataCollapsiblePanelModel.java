package org.helioviewer.jhv.data.gui.info;

import java.util.HashSet;

class DataCollapsiblePanelModel {

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
