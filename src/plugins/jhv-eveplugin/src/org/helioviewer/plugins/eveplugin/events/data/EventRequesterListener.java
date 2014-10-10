package org.helioviewer.plugins.eveplugin.events.data;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

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
    public abstract void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events);
}
