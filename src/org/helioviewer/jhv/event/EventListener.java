package org.helioviewer.jhv.event;

public class EventListener {

    public interface Handle {
        void cacheUpdated();
    }

    public interface Highlight {
        void highlightChanged();
    }

}
