package org.helioviewer.jhv.data.container;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

/**
 * Interface should be implemented by a class that handles received events by
 * the JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie)
 */
public interface JHVEventHandler {
    /**
     * New Events were received by the JHVEventContainer.
     * 
     * @param eventList
     *            the list of events that were received
     */
    public abstract void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList);

    /**
     * Informs the JHVEventHandler the cache was changed.
     * 
     */
    public abstract void cacheUpdated();

}
