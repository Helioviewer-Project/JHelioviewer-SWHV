/**
 * 
 */
package org.helioviewer.jhv.data.datatype.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the relationship between events.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventRelationship {
    /** Events following this event */
    private final List<JHVEventRelation> nextEvents;
    /** Events preceding this event */
    private final List<JHVEventRelation> precedingEvents;
    /** Rules for relation with this event */
    private final List<JHVEventRelationShipRule> relationshipRules;
    /** Related events */
    private final List<JHVEventRelation> relatedEventsByRule;

    /**
     * Default constructor of JHV event relationship.
     */
    public JHVEventRelationship() {
        nextEvents = new ArrayList<JHVEventRelation>();
        precedingEvents = new ArrayList<JHVEventRelation>();
        relationshipRules = new ArrayList<JHVEventRelationShipRule>();
        relatedEventsByRule = new ArrayList<JHVEventRelation>();
    }

    /**
     * Gets the related events by rule.
     * 
     * @return the relatedEventsByRule
     */
    public List<JHVEventRelation> getRelatedEventsByRule() {
        return relatedEventsByRule;
    }

    /**
     * Gets the next events.
     * 
     * @return the nextEvents
     */
    public List<JHVEventRelation> getNextEvents() {
        return nextEvents;
    }

    /**
     * Gets the preceding events.
     * 
     * @return the precedingEvents
     */
    public List<JHVEventRelation> getPrecedingEvents() {
        return precedingEvents;
    }

    /**
     * Gets the relationship rules.
     * 
     * @return the relationshipRules
     */
    public List<JHVEventRelationShipRule> getRelationshipRules() {
        return relationshipRules;
    }

}
