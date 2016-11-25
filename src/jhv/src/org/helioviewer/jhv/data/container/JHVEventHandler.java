package org.helioviewer.jhv.data.container;

import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

/**
 * Interface should be implemented by a class that handles received events by
 * the JHVEventContainer.
 */
public interface JHVEventHandler {

    void cacheUpdated();

    void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events);

}
