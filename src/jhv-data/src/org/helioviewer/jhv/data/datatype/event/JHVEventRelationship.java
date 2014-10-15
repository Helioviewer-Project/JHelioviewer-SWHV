/**
 * 
 */
package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the relationship between events.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventRelationship {
    /** Events following this event */
    private final Map<String, JHVEventRelation> nextEvents;
    /** Events preceding this event */
    private final Map<String, JHVEventRelation> precedingEvents;
    /** Rules for relation with this event */
    private final Map<String, JHVEventRelationShipRule> relationshipRules;
    /** Related events */
    private final Map<String, JHVEventRelation> relatedEventsByRule;
    /**  */
    private Color relationshipColor;

    /**
     * Default constructor of JHV event relationship.
     */
    public JHVEventRelationship() {
        nextEvents = new HashMap<String, JHVEventRelation>();
        precedingEvents = new HashMap<String, JHVEventRelation>();
        relationshipRules = new HashMap<String, JHVEventRelationShipRule>();
        relatedEventsByRule = new HashMap<String, JHVEventRelation>();
        relationshipColor = Color.BLACK;
    }

    /**
     * Gets the related events by rule.
     * 
     * @return the relatedEventsByRule
     */
    public Map<String, JHVEventRelation> getRelatedEventsByRule() {
        return relatedEventsByRule;
    }

    /**
     * Gets the next events.
     * 
     * @return the nextEvents
     */
    public Map<String, JHVEventRelation> getNextEvents() {
        return nextEvents;
    }

    /**
     * Gets the preceding events.
     * 
     * @return the precedingEvents
     */
    public Map<String, JHVEventRelation> getPrecedingEvents() {
        return precedingEvents;
    }

    /**
     * Gets the relationship rules.
     * 
     * @return the relationshipRules
     */
    public Map<String, JHVEventRelationShipRule> getRelationshipRules() {
        return relationshipRules;
    }

    public Color getRelationshipColor() {
        return relationshipColor;
    }

    public void setRelationshipColor(Color color) {
        relationshipColor = color;
        for (JHVEventRelation er : getNextEvents().values()) {
            if (er.getTheEvent() != null) {
                er.getTheEvent().getEventRelationShip().setRelationshipColor(relationshipColor);
            }
        }
    }

    public void merge(JHVEventRelationship eventRelationShip) {
        nextEvents.putAll(eventRelationShip.getNextEvents());
        precedingEvents.putAll(eventRelationShip.getPrecedingEvents());
        relatedEventsByRule.putAll(eventRelationShip.getRelatedEventsByRule());
    }
}
