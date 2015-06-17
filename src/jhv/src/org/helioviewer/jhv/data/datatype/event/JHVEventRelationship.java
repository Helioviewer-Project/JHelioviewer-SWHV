/**
 *
 */
package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final List<JHVEventRelationShipRule> relationshipRules;
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
        relationshipRules = new ArrayList<JHVEventRelationShipRule>();
        relatedEventsByRule = new HashMap<String, JHVEventRelation>();
        relationshipColor = null;
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
    public List<JHVEventRelationShipRule> getRelationshipRules() {
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
        Map<String, JHVEventRelation> otherNextEvents = eventRelationShip.getNextEvents();
        Map<String, JHVEventRelation> otherPrecedingEvents = eventRelationShip.getPrecedingEvents();
        Map<String, JHVEventRelation> otherRulesRelated = eventRelationShip.getRelatedEventsByRule();
        mergeTwoLists(nextEvents, otherNextEvents);
        mergeTwoLists(precedingEvents, otherPrecedingEvents);
        mergeTwoLists(relatedEventsByRule, otherRulesRelated);
    }

    private void mergeTwoLists(Map<String, JHVEventRelation> currentList, Map<String, JHVEventRelation> newList) {
        for (String identifier : newList.keySet()) {
            if (!currentList.containsKey(identifier)) {
                JHVEventRelation newRelatedEvent = newList.get(identifier);
                currentList.put(identifier, newRelatedEvent);
                newRelatedEvent.getTheEvent().getEventRelationShip().setRelationshipColor(relationshipColor);
            }
        }
    }
}
