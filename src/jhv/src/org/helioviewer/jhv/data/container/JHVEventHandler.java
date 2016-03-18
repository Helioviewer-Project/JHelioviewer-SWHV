package org.helioviewer.jhv.data.container;

import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

/**
 * Interface should be implemented by a class that handles received events by
 * the JHVEventContainer.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie)
 */
public interface JHVEventHandler {

    public abstract void cacheUpdated();

    public abstract void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>> events);

}
