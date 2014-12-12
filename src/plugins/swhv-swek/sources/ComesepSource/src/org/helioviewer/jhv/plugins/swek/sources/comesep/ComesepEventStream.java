package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.LinkedList;
import java.util.Queue;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;

public class ComesepEventStream implements SWEKEventStream {

    /** Queue with events */
    Queue<JHVEvent> eventQueue = new LinkedList<JHVEvent>();

    public ComesepEventStream() {
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

    @Override
    public boolean additionalDownloadNeeded() {
        return false;
    }

    public void addJHVEvent(ComesepEvent currentEvent) {
        eventQueue.add(currentEvent);
    }

}
