/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a relationship between events.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedEvents {
    /** The source event */
    private SWEKEventType event;

    /** The related event */
    private SWEKEventType relatedWith;

    /** The parameters on which they are related */
    private List<SWEKRelatedOn> relatedOnList;

    /**
     * Creates a related events with no source event, no related event and an empty related on list.
     */
    public SWEKRelatedEvents() {
        super();
        this.event = null;
        this.relatedWith = null;
        this.relatedOnList = new ArrayList<SWEKRelatedOn>();
    }

    /**
     * Creates a related on relation between the source event and the related with event with the given related on list.
     *
     * @param event             The source event in the relation
     * @param relatedWith       The related event
     * @param relatedOnList     The list of parameters on which the related events are related
     */
    public SWEKRelatedEvents(SWEKEventType event, SWEKEventType relatedWith, List<SWEKRelatedOn> relatedOnList) {
        super();
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
     * Sets the source event
     *
     * @param event the event to set
     */
    public void setEvent(SWEKEventType event) {
        this.event = event;
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
     * Sets the related event
     *
     * @param relatedWith the relatedWith to set
     */
    public void setRelatedWith(SWEKEventType relatedWith) {
        this.relatedWith = relatedWith;
    }

    /**
     * Gets the list of corresponding event parameters.
     *
     * @return the relatedOnList
     */
    public List<SWEKRelatedOn> getRelatedOnList() {
        return relatedOnList;
    }

    /**
     * Sets the list of corresponding event parameters.
     *
     * @param relatedOnList the relatedOnList to set
     */
    public void setRelatedOnList(List<SWEKRelatedOn> relatedOnList) {
        this.relatedOnList = relatedOnList;
    }
}
