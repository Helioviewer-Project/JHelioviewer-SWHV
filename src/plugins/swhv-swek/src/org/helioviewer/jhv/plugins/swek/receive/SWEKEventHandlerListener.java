package org.helioviewer.jhv.plugins.swek.receive;

import java.util.List;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

/**
 * All classes interested in new events should implement the
 * SWEKEventHandlerListener interface and register with the SWEKEventHandler.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface SWEKEventHandlerListener {
    /**
     * New events were received.
     * 
     * @param events
     *            the events received
     */
    public abstract void newEventReceived(List<JHVEvent> events);
}
