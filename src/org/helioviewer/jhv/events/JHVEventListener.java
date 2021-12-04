package org.helioviewer.jhv.events;

public class JHVEventListener {

    // Implemented by a class that handles received events
    public interface Handle {
        void cacheUpdated();
        void newEventsReceived();
    }

    public interface Highlight {
        void highlightChanged();
    }

}
