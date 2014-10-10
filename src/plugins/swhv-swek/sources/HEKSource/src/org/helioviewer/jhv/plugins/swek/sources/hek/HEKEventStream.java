package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.LinkedList;
import java.util.Queue;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;

/**
 * A stream of Hek events. Implementation of the SWEKEventStream.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class HEKEventStream implements SWEKEventStream {
    /** Queue with events */
    Queue<JHVEvent> eventQueue = new LinkedList<JHVEvent>();

    public HEKEventStream() {
        eventQueue = new LinkedList<JHVEvent>();
    }

    @Override
    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    @Override
    public JHVEvent next() {
        return eventQueue.poll();
    }

    /**
     * Adds an event to the stream.
     * 
     * @param event
     */
    public void addJHVEvent(JHVEvent event) {
        eventQueue.add(event);
    }
}
