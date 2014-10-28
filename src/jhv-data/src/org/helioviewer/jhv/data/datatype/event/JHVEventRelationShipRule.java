/**
 * 
 */
package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

/**
 * Defines a relationship rule that can be used by the JHVeventContainer to link
 * events.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventRelationShipRule {
    /** The event type of the relationship rule */
    private JHVEventType relatedWith;
    /** The event parameter on which the events are related */
    private final List<JHVRelatedOn> relatedOn;

    /**
     * Creates a JHV event relationship rule for the given event type and
     * parameter.
     * 
     * @param relatedWith
     *            related with what event type
     * @param relatedOn
     *            related on what event parameter
     */
    public JHVEventRelationShipRule(JHVEventType relatedWith, List<JHVRelatedOn> relatedOn) {
        this.relatedWith = relatedWith;
        this.relatedOn = relatedOn;
    }

    /**
     * With what event type does this event relate.
     * 
     * @return the relatedWith
     */
    public JHVEventType getRelatedWith() {
        return relatedWith;
    }

    /**
     * Set with what event type this event relates
     * 
     * @param relatedWith
     *            the relatedWith to set
     */
    public void setRelatedWith(JHVEventType relatedWith) {
        this.relatedWith = relatedWith;
    }

    /**
     * Get on what parameters the event types relate.
     * 
     * @return the relatedOn
     */
    public List<JHVRelatedOn> getRelatedOn() {
        return relatedOn;
    }
}
