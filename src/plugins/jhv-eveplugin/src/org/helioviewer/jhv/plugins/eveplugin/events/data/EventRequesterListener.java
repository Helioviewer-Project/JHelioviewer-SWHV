package org.helioviewer.jhv.plugins.eveplugin.events.data;

import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

/**
 * All classes interested in JHVEvents should implement this interface and
 * register with the IncomingEventHandler.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public interface EventRequesterListener {
    /**
     * New Events where received.
     *
     * @param events
     *            the events received
     */
    public abstract void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events);
}
