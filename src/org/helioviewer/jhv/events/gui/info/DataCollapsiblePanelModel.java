package org.helioviewer.jhv.events.gui.info;

import java.util.HashSet;

class DataCollapsiblePanelModel {

    private final HashSet<DataCollapsiblePanelModelListener> listeners = new HashSet<>();

    void repackCollapsiblePanels() {
        listeners.forEach(DataCollapsiblePanelModelListener::repack);
    }

    void addListener(DataCollapsiblePanelModelListener listener) {
        listeners.add(listener);
    }

    void removeListener(DataCollapsiblePanelModelListener listener) {
        listeners.remove(listener);
    }

}
