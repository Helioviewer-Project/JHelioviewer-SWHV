package org.helioviewer.jhv.plugins.swek.config;

import java.util.List;

/**
 * Defines a relationship between events.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedEvents {

    /** The source event */
    private final SWEKEventType event;

    /** The related event */
    private final SWEKEventType relatedWith;

    /** The parameters on which they are related */
    private final List<SWEKRelatedOn> relatedOnList;

    /**
     * Creates a related on relation between the source event and the related with event with the given related on list.
     *
     * @param event             The source event in the relation
     * @param relatedWith       The related event
     * @param relatedOnList     The list of parameters on which the related events are related
     */
    public SWEKRelatedEvents(SWEKEventType event, SWEKEventType relatedWith, List<SWEKRelatedOn> relatedOnList) {
        this.event = event;
        this.relatedWith = relatedWith;
        this.relatedOnList = relatedOnList;
    }

    /**
     * Gets the source event
     *
     * @return the event
     */
    public SWEKEventType getEvent() {
        return event;
    }

    /**
     * Gets the related event
     *
     * @return the relatedWith
     */
    public SWEKEventType getRelatedWith() {
        return relatedWith;
    }

    /**
     * Gets the list of corresponding event parameters.
     *
     * @return the relatedOnList
     */
    public List<SWEKRelatedOn> getRelatedOnList() {
        return relatedOnList;
    }

}
