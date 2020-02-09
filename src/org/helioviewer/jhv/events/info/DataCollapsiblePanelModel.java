package org.helioviewer.jhv.events.info;

import java.util.ArrayList;

class DataCollapsiblePanelModel {

    private final ArrayList<DataCollapsiblePanelModelListener> listeners = new ArrayList<>();

    void addListener(DataCollapsiblePanelModelListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    void repackCollapsiblePanels() {
        listeners.forEach(DataCollapsiblePanelModelListener::repack);
    }

}
