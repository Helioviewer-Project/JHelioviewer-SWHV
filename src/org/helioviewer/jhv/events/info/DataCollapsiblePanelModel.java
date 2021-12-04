package org.helioviewer.jhv.events.info;

import java.util.ArrayList;

class DataCollapsiblePanelModel {

    interface Listener {
        void repack();
    }

    private final ArrayList<Listener> listeners = new ArrayList<>();

    void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    void repackCollapsiblePanels() {
        listeners.forEach(Listener::repack);
    }

}
