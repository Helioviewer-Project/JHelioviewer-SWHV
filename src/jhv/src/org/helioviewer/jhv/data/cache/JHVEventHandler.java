package org.helioviewer.jhv.data.cache;

import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.event.JHVEventType;

// Interface should be implemented by a class that handles received events by the JHVEventContainer
public interface JHVEventHandler {

    void cacheUpdated();

    void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events);

}
