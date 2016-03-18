package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.LinkedList;
import java.util.Queue;

import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;

public class ComesepEventStream implements SWEKEventStream {

    /** Queue with events */
    private final Queue<JHVEvent> eventQueue = new LinkedList<JHVEvent>();
    private final LinkedList<JHVAssociation> associationQueue = new LinkedList<JHVAssociation>();

    @Override
    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    @Override
    public JHVEvent next() {
        return eventQueue.poll();
    }

    @Override
    public boolean hasAssociations() {
        return !associationQueue.isEmpty();
    }

    @Override
    public JHVAssociation nextAssociation() {
        return associationQueue.poll();
    }

    public void addJHVAssociation(JHVAssociation association) {
        associationQueue.add(association);

    }

    @Override
    public boolean additionalDownloadNeeded() {
        return false;
    }

    public void addJHVEvent(ComesepEvent currentEvent) {
        eventQueue.add(currentEvent);
    }

}
