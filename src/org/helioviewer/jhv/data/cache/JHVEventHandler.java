package org.helioviewer.jhv.data.cache;

// Interface should be implemented by a class that handles received events by the JHVEventContainer
public interface JHVEventHandler {

    void cacheUpdated();

    void newEventsReceived();

}
