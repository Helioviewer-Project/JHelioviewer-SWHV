package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.LinkedList;

import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;

/**
 * A stream of Hek events. Implementation of the SWEKEventStream.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class HEKEventStream implements SWEKEventStream {

    private final LinkedList<JHVEvent> eventQueue = new LinkedList<JHVEvent>();
    private final LinkedList<JHVAssociation> associationQueue = new LinkedList<JHVAssociation>();

    private boolean extraDownloadNeeded;

    @Override
    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    @Override
    public JHVEvent next() {
        return eventQueue.poll();
    }

    @Override
    public JHVAssociation nextAssociation() {
        return associationQueue.poll();
    }

    public void addJHVEvent(JHVEvent event) {
        eventQueue.add(event);
    }

    public void addJHVAssociation(JHVAssociation association) {
        associationQueue.add(association);

    }

    @Override
    public boolean additionalDownloadNeeded() {
        return extraDownloadNeeded;
    }

    /**
     * Sets if extra download is needed.
     *
     * @param extraNeeded
     *            true if extra download is needed, false if not.
     */
    public void setExtraDownloadNeeded(boolean extraNeeded) {
        extraDownloadNeeded = extraNeeded;
    }

}
