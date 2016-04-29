package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

/**
 * Defines a relationship between events.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedEvents {

    private final SWEKEventType event;

    private final SWEKEventType relatedWith;

    private final List<SWEKRelatedOn> relatedOnList;

    public SWEKRelatedEvents(SWEKEventType event, SWEKEventType relatedWith, List<SWEKRelatedOn> relatedOnList) {
        this.event = event;
        this.relatedWith = relatedWith;
        this.relatedOnList = relatedOnList;
    }

    public SWEKEventType getEvent() {
        return event;
    }

    public SWEKEventType getRelatedWith() {
        return relatedWith;
    }

    public List<SWEKRelatedOn> getRelatedOnList() {
        return relatedOnList;
    }

}
