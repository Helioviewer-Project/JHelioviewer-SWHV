package org.helioviewer.jhv.data.container.cache;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.helioviewer.base.cache.RequestCache;
import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.data.container.util.DateUtil;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelationShipRule;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedOn;

public class JHVEventCache {
    /** singleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    private final Map<Date, Map<Date, List<JHVEvent>>> events;

    /** A set with IDs */
    private final Set<String> eventIDs;

    private final Map<String, JHVEvent> allEvents;

    private final Map<String, List<JHVEvent>> missingEventsInEventRelations;

    private final List<JHVEvent> eventsWithRelationRules;

    private final Set<JHVEventType> activeEventTypes;

    private final Map<JHVEventType, RequestCache> downloadedCache;

    private final Map<String, Color> colorPerId;

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<Date, Map<Date, List<JHVEvent>>>();
        eventIDs = new HashSet<String>();
        allEvents = new HashMap<String, JHVEvent>();
        missingEventsInEventRelations = new HashMap<String, List<JHVEvent>>();
        eventsWithRelationRules = new ArrayList<JHVEvent>();
        activeEventTypes = new HashSet<JHVEventType>();
        downloadedCache = new HashMap<JHVEventType, RequestCache>();
        colorPerId = new HashMap<String, Color>();
    }

    /**
     * Gets the singleton instance of the JHV event cache.
     *
     * @return singleton instance of the cache.
     */
    public static JHVEventCache getSingletonInstance() {
        if (instance == null) {
            instance = new JHVEventCache();
        }
        return instance;
    }

    /**
     * Add the JHV event to the cache
     *
     * @param event
     *            the event to add
     */
    public void add(JHVEvent event) {
        activeEventTypes.add(event.getJHVEventType());
        if (!eventIDs.contains(event.getUniqueID())) {
            allEvents.put(event.getUniqueID(), event);
            Date startDate = DateUtil.getCurrentDate(event.getStartDate());
            Date endDate = DateUtil.getNextDate(event.getEndDate());
            addToList(startDate, endDate, event);
            eventIDs.add(event.getUniqueID());
            checkAndFixRelationShip(event);
        } else {
            JHVEvent savedEvent = allEvents.get(event.getUniqueID());
            savedEvent.merge(event);
            checkAndFixRelationShip(savedEvent);

        }
    }

    /**
     * Gets the events that are available within the interval.
     *
     * @param startDate
     *            start date of the interval
     * @param endDate
     *            end date of the interval
     * @return the list of events that are available in the interval
     */
    public JHVEventCacheResult get(Date startDate, Date endDate, Date extendedStart, Date extendedEnd) {
        Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
        if (!activeEventTypes.isEmpty()) {
            Calendar intervalS = Calendar.getInstance();
            intervalS.setTime(startDate);
            Calendar intervalE = Calendar.getInstance();
            intervalE.setTime(endDate);
            for (Date sDate : events.keySet()) {
                Calendar tempS = Calendar.getInstance();
                tempS.setTime(sDate);
                // event starts after interval start but before interval end
                if (intervalS.compareTo(tempS) <= 0 && intervalE.compareTo(tempS) >= 0) {
                    for (List<JHVEvent> tempEvent : events.get(sDate).values()) {
                        addEventsToResult(eventsResult, tempEvent);
                    }
                }
                // event start before interval start and end after interval
                // start
                Map<Date, List<JHVEvent>> endDatesEvents = events.get(sDate);
                for (Date eDate : endDatesEvents.keySet()) {
                    Calendar tempE = Calendar.getInstance();
                    tempE.setTime(eDate);
                    if (intervalS.compareTo(tempS) >= 0 && intervalS.compareTo(tempE) <= 0) {
                        addEventsToResult(eventsResult, endDatesEvents.get(eDate));
                    }
                }

            }
        }

        Map<JHVEventType, List<Interval<Date>>> missingIntervals = new HashMap<JHVEventType, List<Interval<Date>>>();
        for (JHVEventType evt : activeEventTypes) {
            List<Interval<Date>> missing = downloadedCache.get(evt).getMissingIntervals(new Interval<Date>(startDate, endDate));
            if (!missing.isEmpty()) {
                missing = downloadedCache.get(evt).adaptRequestCache(extendedStart, extendedEnd);
            }
            missingIntervals.put(evt, missing);
        }
        return new JHVEventCacheResult(eventsResult, missingIntervals);
    }

    /**
     * Removes all the events of the given event type from the event cache.
     *
     * @param eventType
     *            the event type to remove
     */
    public void removeEventType(JHVEventType eventType, boolean keepActive) {
        if (!keepActive) {
            activeEventTypes.remove(eventType);
        } else {
            deleteFromCache(eventType);
        }
    }

    private void deleteFromCache(JHVEventType eventType) {
        downloadedCache.put(eventType, new RequestCache());
        for (Iterator<Map.Entry<String, JHVEvent>> it = allEvents.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, JHVEvent> entry = it.next();
            if (entry.getValue().getJHVEventType().equals(eventType)) {
                eventIDs.remove(entry.getKey());
                colorPerId.remove(entry.getKey());
                eventsWithRelationRules.remove(entry.getValue());
                missingEventsInEventRelations.remove(entry.getKey());
                it.remove();
            }
        }
        for (Iterator<Map.Entry<Date, Map<Date, List<JHVEvent>>>> itDate1 = events.entrySet().iterator(); itDate1.hasNext();) {
            for (Iterator<Map.Entry<Date, List<JHVEvent>>> itDate2 = itDate1.next().getValue().entrySet().iterator(); itDate2.hasNext();) {
                for (Iterator<JHVEvent> itEvent = itDate2.next().getValue().iterator(); itEvent.hasNext();) {
                    if (itEvent.next().getJHVEventType().equals(eventType)) {
                        itEvent.remove();
                    }
                }
            }
        }
    }

    private void checkAndFixRelationShip(JHVEvent event) {
        checkMissingRelations(event);
        checkAndFixRelatedEvents(event, event.getEventRelationShip().getNextEvents().values());
        checkAndFixRelatedEvents(event, event.getEventRelationShip().getPrecedingEvents().values());
        checkAndFixRelatedEvents(event, event.getEventRelationShip().getRelatedEventsByRule().values());
        executeRelationshipRules(event);
        checkRelationColor(event);
    }

    private void executeRelationshipRules(JHVEvent event) {
        List<JHVEventRelationShipRule> rules = event.getEventRelationShip().getRelationshipRules();
        for (JHVEventRelationShipRule rule : rules) {
            // Go over the rules
            for (JHVEvent candidate : eventsWithRelationRules) {
                // Check new candidate
                if (candidate.getJHVEventType().getEventType().toLowerCase().equals(rule.getRelatedWith().getEventType().toLowerCase())) {
                    // Candidate has the correct event type
                    int foundCorrespondinParameters = 0;
                    for (JHVRelatedOn relatedOn : rule.getRelatedOn()) {
                        // Check all the related on parameters
                        if (candidate.getAllEventParameters().containsKey(relatedOn.getRelatedOnWith().getParameterName().toLowerCase())) {
                            JHVEventParameter p = candidate.getAllEventParameters().get(relatedOn.getRelatedOnWith().getParameterName().toLowerCase());
                            // Candidate has the related on parameter
                            if (event.getAllEventParameters().containsKey(relatedOn.getRelatedOnWith().getParameterName().toLowerCase())) {
                                JHVEventParameter eventP = event.getAllEventParameters().get(relatedOn.getRelatedOnWith().getParameterName().toLowerCase());
                                if (eventP.getParameterName().toLowerCase().equals(relatedOn.getRelatedOnWith().getParameterName().toLowerCase())) {
                                    // Parameter found in the event
                                    if (eventP.getParameterValue() != null && p.getParameterValue() != null && eventP.getParameterValue().equals(p.getParameterValue())) {
                                        // at least one of the related on
                                        // parameters found
                                        foundCorrespondinParameters++;
                                    }
                                } else {
                                    // parameter not found in the event skip
                                    // rule.
                                }
                            }
                        }
                    }
                    if (foundCorrespondinParameters == rule.getRelatedOn().size()) {
                        event.getEventRelationShip().getRelatedEventsByRule().put(candidate.getUniqueID(), new JHVEventRelation(candidate.getUniqueID(), candidate));
                        candidate.getEventRelationShip().getRelatedEventsByRule().put(event.getUniqueID(), new JHVEventRelation(event.getUniqueID(), event));
                    }
                }
            }
        }
        eventsWithRelationRules.add(event);
    }

    private void checkRelationColor(JHVEvent event) {
        Color c = containsColoredRelation(event, new HashSet<String>());
        if (c == null) {
            c = JHVCacheColors.getNextColor();
        }
        colorRelationship(event, c, new HashSet<String>());
    }

    private void colorRelationship(JHVEvent event, Color c, HashSet<String> handledEvents) {
        if (!handledEvents.contains(event.getUniqueID())) {
            handledEvents.add(event.getUniqueID());
            event.getEventRelationShip().setRelationshipColor(c);
            colorPerId.put(event.getUniqueID(), c);
            colorRelationship(event.getEventRelationShip().getNextEvents().values(), c, handledEvents);
            colorRelationship(event.getEventRelationShip().getPrecedingEvents().values(), c, handledEvents);
            colorRelationship(event.getEventRelationShip().getRelatedEventsByRule().values(), c, handledEvents);
        }
    }

    private void colorRelationship(Collection<JHVEventRelation> eventRelations, Color c, HashSet<String> handledEvents) {
        for (JHVEventRelation evRel : eventRelations) {
            if (evRel.getTheEvent() != null) {
                colorRelationship(evRel.getTheEvent(), c, handledEvents);
            }
        }
    }

    private Color containsColoredRelation(JHVEvent event, HashSet<String> handledEvents) {
        if (colorPerId.containsKey(event.getUniqueID())) {
            return colorPerId.get(event.getUniqueID());
        }
        Color c = containsColoredRelation(event.getEventRelationShip().getNextEvents().values(), handledEvents);
        if (c != null) {
            return c;
        }
        c = containsColoredRelation(event.getEventRelationShip().getPrecedingEvents().values(), handledEvents);
        if (c != null) {
            return c;
        }
        c = containsColoredRelation(event.getEventRelationShip().getRelatedEventsByRule().values(), handledEvents);
        return c;
    }

    private Color containsColoredRelation(Collection<JHVEventRelation> eventRelations, HashSet<String> handledEvents) {
        for (JHVEventRelation evRel : eventRelations) {
            if (evRel.getTheEvent() != null) {
                if (!handledEvents.contains(evRel.getTheEvent().getUniqueID())) {
                    handledEvents.add(evRel.getTheEvent().getUniqueID());
                    if (colorPerId.containsKey(evRel.getTheEvent().getUniqueID())) {
                        return colorPerId.get(evRel.getTheEvent().getUniqueID());
                    }
                    Color c = containsColoredRelation(evRel.getTheEvent(), handledEvents);
                    if (c != null) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    private void checkMissingRelations(JHVEvent event) {
        if (missingEventsInEventRelations.containsKey(event.getUniqueID())) {
            List<JHVEvent> listOfRelatedEvents = missingEventsInEventRelations.get(event.getUniqueID());
            for (JHVEvent relatedEvent : listOfRelatedEvents) {
                if (relatedEvent.getEventRelationShip().getNextEvents().containsKey(event.getUniqueID())) {
                    JHVEventRelation relation = relatedEvent.getEventRelationShip().getNextEvents().get(event.getUniqueID());
                    relation.setTheEvent(event);
                    // event.getEventRelationShip().setRelationshipColor(relatedEvent.getColor());
                    // it might be possible there is no definition in the
                    // current event for the relationship with the related
                    // event. So we add a preceding event relationship.
                    Map<String, JHVEventRelation> precedingEvents = event.getEventRelationShip().getPrecedingEvents();
                    if (!precedingEvents.containsKey(relatedEvent.getUniqueID())) {
                        precedingEvents.put(relatedEvent.getUniqueID(), new JHVEventRelation(relatedEvent.getUniqueID(), relatedEvent));
                    }
                }
                if (relatedEvent.getEventRelationShip().getPrecedingEvents().containsKey(event.getUniqueID())) {
                    JHVEventRelation relation = relatedEvent.getEventRelationShip().getPrecedingEvents().get(event.getUniqueID());
                    relation.setTheEvent(event);
                    // event.getEventRelationShip().setRelationshipColor(relatedEvent.getEventRelationShip().getRelationshipColor());
                    // it might be possible there is no definition in the
                    // current event for the relationship with the related
                    // event. So we add a next event relationship.
                    Map<String, JHVEventRelation> nextEvents = event.getEventRelationShip().getNextEvents();
                    if (!nextEvents.containsKey(relatedEvent.getUniqueID())) {
                        nextEvents.put(relatedEvent.getUniqueID(), new JHVEventRelation(relatedEvent.getUniqueID(), relatedEvent));
                    }
                }
            }
            missingEventsInEventRelations.remove(event.getUniqueID());
        }
    }

    private void checkAndFixRelatedEvents(JHVEvent event, Collection<JHVEventRelation> eventRelationCollection) {
        for (JHVEventRelation er : eventRelationCollection) {
            if (er.getTheEvent() == null) {
                if (allEvents.containsKey(er.getUniqueIdentifier())) {
                    er.setTheEvent(allEvents.get(er.getUniqueIdentifier()));
                } else {
                    List<JHVEvent> missingRelations = new ArrayList<JHVEvent>();
                    if (missingEventsInEventRelations.containsKey(er.getUniqueIdentifier())) {
                        missingRelations = missingEventsInEventRelations.get(er.getUniqueIdentifier());
                    }
                    missingRelations.add(event);
                    missingEventsInEventRelations.put(er.getUniqueIdentifier(), missingRelations);
                }
            } else {
                if (allEvents.containsKey(er.getUniqueIdentifier())) {
                    JHVEvent savedEvent = allEvents.get(er.getUniqueIdentifier());
                    er.setTheEvent(savedEvent);
                }
            }
        }
    }

    /**
     * Adds the event with cache date start date and end date to the event
     * cache.
     *
     * @param startDate
     *            the cache start date of the event
     * @param endDate
     *            the cache end date of the event
     * @param event
     *            the event to add
     */
    private void addToList(Date startDate, Date endDate, JHVEvent event) {
        Map<Date, List<JHVEvent>> eventsOnStartDate = new HashMap<Date, List<JHVEvent>>();
        List<JHVEvent> eventsOnStartEndDate = new ArrayList<JHVEvent>();
        if (events.containsKey(startDate)) {
            eventsOnStartDate = events.get(startDate);
            if (eventsOnStartDate.containsKey(endDate)) {
                eventsOnStartEndDate = eventsOnStartDate.get(endDate);
            }
        }
        eventsOnStartEndDate.add(event);
        eventsOnStartDate.put(endDate, eventsOnStartEndDate);
        events.put(startDate, eventsOnStartDate);
    }

    /**
     * Adds a list of events to the given result.
     *
     * @param eventsResult
     *            the result to which the data should be added
     * @param tempEvents
     *            the events that should be added
     */
    private void addEventsToResult(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult, List<JHVEvent> tempEvents) {
        for (JHVEvent event : tempEvents) {
            if (activeEventTypes.contains(event.getJHVEventType())) {
                NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>> datesPerType = new TreeMap<Date, NavigableMap<Date, List<JHVEvent>>>();
                NavigableMap<Date, List<JHVEvent>> endDatesPerStartDate = new TreeMap<Date, List<JHVEvent>>();
                List<JHVEvent> events = new ArrayList<JHVEvent>();
                if (eventsResult.containsKey(event.getJHVEventType().getEventType())) {
                    datesPerType = eventsResult.get(event.getJHVEventType().getEventType());
                    if (datesPerType.containsKey(event.getStartDate())) {
                        endDatesPerStartDate = datesPerType.get(event.getStartDate());
                        if (endDatesPerStartDate.containsKey(event.getEndDate())) {
                            events = endDatesPerStartDate.get(event.getEndDate());
                        }
                    }
                }
                events.add(event);
                endDatesPerStartDate.put(event.getEndDate(), events);
                datesPerType.put(event.getStartDate(), endDatesPerStartDate);
                eventsResult.put(event.getJHVEventType().getEventType(), datesPerType);
            }
        }
    }

    public Collection<Interval<Date>> getAllRequestIntervals(JHVEventType eventType) {
        return downloadedCache.get(eventType).getAllRequestIntervals();
    }

    public void removeRequestedIntervals(JHVEventType eventType, Interval<Date> interval) {
        downloadedCache.get(eventType).removeRequestedIntervals(interval);

    }

    public void eventTypeActivated(JHVEventType eventType) {
        activeEventTypes.add(eventType);
        if (!downloadedCache.containsKey(eventType)) {
            downloadedCache.put(eventType, new RequestCache());
        }
    }
}