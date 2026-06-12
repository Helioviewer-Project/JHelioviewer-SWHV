package org.helioviewer.jhv.event;

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
