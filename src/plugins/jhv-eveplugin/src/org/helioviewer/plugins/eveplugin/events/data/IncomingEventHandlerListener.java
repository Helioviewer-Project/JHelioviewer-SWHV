package org.helioviewer.plugins.eveplugin.events.data;

import java.util.List;

import org.helioviewer.jhv.data.datatype.JHVEvent;

/**
 * All classes interested in JHVEvents should implement this interface and
 * register with the IncomingEventHandler.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface IncomingEventHandlerListener {
    /**
     * New Events where received.
     * 
     * @param events
     *            the events received
     */
    public abstract void newEventsReceived(List<JHVEvent> events);
}
