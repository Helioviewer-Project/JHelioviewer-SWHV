/**
 * 
 */
package org.helioviewer.jhv.data.datatype.event;

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
    private JHVEventParameter relatedOn;

    /**
     * Creates a JHV event relationship rule for the given event type and
     * parameter.
     * 
     * @param relatedWith
     *            related with what event type
     * @param relatedOn
     *            related on what event parameter
     */
    public JHVEventRelationShipRule(JHVEventType relatedWith, JHVEventParameter relatedOn) {
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
     * Get on what parameter the event types relate.
     * 
     * @return the relatedOn
     */
    public JHVEventParameter getRelatedOn() {
        return relatedOn;
    }

    /**
     * Set on what parameter the event types relate.
     * 
     * @param relatedOn
     *            the relatedOn to set
     */
    public void setRelatedOn(JHVEventParameter relatedOn) {
        this.relatedOn = relatedOn;
    }

}
