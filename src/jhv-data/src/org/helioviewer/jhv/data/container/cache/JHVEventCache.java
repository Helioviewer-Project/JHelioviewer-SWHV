package org.helioviewer.jhv.data.container.cache;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.helioviewer.jhv.data.container.util.DateUtil;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelationShipRule;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedOn;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

public class JHVEventCache {
    /** singleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    private final Map<Date, Map<Date, List<JHVEvent>>> events;

    /** A set with IDs */
    private final Set<String> eventIDs;

    private final Map<String, JHVEvent> allEvents;

    private final Map<String, List<JHVEvent>> missingEventsInEventRelations;

    private final Map<Color, Set<String>> idsPerColor;

    private final List<JHVEvent> eventsWithRelationRules;

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<Date, Map<Date, List<JHVEvent>>>();
        eventIDs = new HashSet<String>();
        allEvents = new HashMap<String, JHVEvent>();
        missingEventsInEventRelations = new HashMap<String, List<JHVEvent>>();
        idsPerColor = new HashMap<Color, Set<String>>();
        eventsWithRelationRules = new ArrayList<JHVEvent>();
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
        synchronized (JHVEventContainerLocks.cacheLock) {
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
            }
        }

    }

    /**
     * Gets the events containing the given date.
     * 
     * @param date
     *            The date in which the event should have happened
     * @return the list of events happened on the given date
     */
    /*
     * public List<JHVEvent> get(Date date) { synchronized
     * (JHVEventContainerLocks.cacheLock) { List<JHVEvent> eventsResult = new
     * ArrayList<JHVEvent>(); Calendar reference = Calendar.getInstance();
     * reference.setTime(date); for (Date sDate : events.keySet()) { Calendar
     * tempS = Calendar.getInstance(); tempS.setTime(sDate); if
     * (tempS.compareTo(reference) <= 0) { Map<Date, List<JHVEvent>>
     * eventOnEndTime = events.get(sDate); for (Date eDate :
     * eventOnEndTime.keySet()) { Calendar tempE = Calendar.getInstance();
     * tempE.setTime(eDate); if (tempE.compareTo(reference) >= 0) {
     * eventsResult.addAll(eventOnEndTime.get(eDate)); } } } } return
     * eventsResult; } }
     */
    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> get(Date date) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
            Calendar reference = Calendar.getInstance();
            reference.setTime(date);
            for (Date sDate : events.keySet()) {
                Calendar tempS = Calendar.getInstance();
                tempS.setTime(sDate);
                if (tempS.compareTo(reference) <= 0) {
                    Map<Date, List<JHVEvent>> eventOnEndTime = events.get(sDate);
                    for (Date eDate : eventOnEndTime.keySet()) {
                        Calendar tempE = Calendar.getInstance();
                        tempE.setTime(eDate);
                        if (tempE.compareTo(reference) >= 0) {
                            addEventsToResult(eventsResult, eventOnEndTime.get(eDate));
                        }
                    }
                }
            }
            return eventsResult;
        }
    }

    /**
     * Gets the events containing on of the dates in the list of date.
     * 
     * @param dates
     *            list of dates for which events are requested
     * @return the list of events that are available for the dates
     */
    /*
     * public List<JHVEvent> get(List<Date> dates) { synchronized
     * (JHVEventContainerLocks.cacheLock) { List<JHVEvent> events = new
     * ArrayList<JHVEvent>(); for (Date date : dates) {
     * events.addAll(get(date)); } return events; } }
     */
    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> get(List<Date> dates) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
            for (Date date : dates) {
                eventsResult = mergeMaps(eventsResult, get(date));
            }
            return eventsResult;
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
    /*
     * public List<JHVEvent> get(Date startDate, Date endDate) { synchronized
     * (JHVEventContainerLocks.cacheLock) { List<JHVEvent> eventsResult = new
     * ArrayList<JHVEvent>(); Calendar intervalS = Calendar.getInstance();
     * intervalS.setTime(startDate); Calendar intervalE =
     * Calendar.getInstance(); intervalE.setTime(endDate); for (Date sDate :
     * events.keySet()) { Calendar tempS = Calendar.getInstance();
     * tempS.setTime(sDate); // event starts after interval start but before
     * interval end if (intervalS.compareTo(tempS) <= 0 &&
     * intervalE.compareTo(tempS) >= 0) { for (List<JHVEvent> tempEvent :
     * events.get(sDate).values()) { eventsResult.addAll(tempEvent); } } //
     * event start before interval start and end after interval // start
     * Map<Date, List<JHVEvent>> endDatesEvents = events.get(sDate); for (Date
     * eDate : endDatesEvents.keySet()) { Calendar tempE =
     * Calendar.getInstance(); tempE.setTime(eDate); if
     * (intervalS.compareTo(tempS) >= 0 && intervalS.compareTo(tempE) <= 0) {
     * eventsResult.addAll(endDatesEvents.get(eDate)); } }
     * 
     * } return eventsResult; } }
     */
    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> get(Date startDate, Date endDate) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
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
            return eventsResult;
        }
    }

    /**
     * Removes all the events of the given event type from the event cache.
     * 
     * @param eventType
     *            the event type to remove
     */
    public void removeEventType(JHVEventType eventType) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            for (Map<Date, List<JHVEvent>> endDateEvents : events.values()) {
                for (List<JHVEvent> eventList : endDateEvents.values()) {
                    List<JHVEvent> deleteList = new ArrayList<JHVEvent>();
                    for (JHVEvent event : eventList) {
                        if (event.getJHVEventType().equals(eventType)) {
                            deleteList.add(event);
                            eventIDs.remove(event.getUniqueID());
                            allEvents.remove(event.getUniqueID());
                            missingEventsInEventRelations.remove(event.getUniqueID());
                            eventsWithRelationRules.remove(event);
                            for (JHVEventRelation relation : event.getEventRelationShip().getRelatedEventsByRule().values()) {
                                if (relation.getTheEvent() != null) {
                                    relation.getTheEvent().getEventRelationShip().getRelatedEventsByRule().remove(event.getUniqueID());
                                }
                            }
                        }
                    }
                    eventList.removeAll(deleteList);
                }
            }
        }

    }

    private void checkAndFixRelationShip(JHVEvent event) {
        checkRelationColor(event);
        checkMissingRelations(event);
        checkAndFixNextRelatedEvents(event);
        checkAndFixPrecedingRelatedEvents(event);
        checkAndFixRelatedEventsByRule(event);
        executeRelationshipRules(event);
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
                        for (JHVEventParameter p : candidate.getAllEventParameters()) {
                            // Loop over candidate parameters
                            if (relatedOn.getRelatedOnWith().getParameterName().toLowerCase().equals(p.getParameterName().toLowerCase())) {
                                // Candidate has the related on parameter
                                for (JHVEventParameter eventP : event.getAllEventParameters()) {
                                    // Loop over the event parameter to find the
                                    // value of the related on parameter
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
        if (event.getEventRelationShip().getRelationshipColor() == null) {
            event.getEventRelationShip().setRelationshipColor(JHVCacheColors.getNextColor());
        }
        if (idsPerColor.containsKey(event.getEventRelationShip().getRelationshipColor())) {
            // Color of event is already used check if the event is in the
            // list
            // of events that can use the color
            Set<String> ids = idsPerColor.get(event.getEventRelationShip().getRelationshipColor());
            if (ids.contains(event.getUniqueID())) {
                // The event is in the list of events, add the following and
                // preceding event ids to the list and give them the correct
                // color
                addRelatedEvents(event, new HashSet<String>());
            } else {
                // The event is not in the list check if one of the
                // following or
                // preceding events is in the list
                if (checkFollowingAndPrecedingEvents(event)) {
                    addRelatedEvents(event, new HashSet<String>());
                } else {
                    Color c = JHVCacheColors.getNextColor();
                    event.getEventRelationShip().setRelationshipColor(c);
                    ids = new HashSet<String>();
                    ids.add(event.getUniqueID());
                    idsPerColor.put(event.getEventRelationShip().getRelationshipColor(), ids);
                    addRelatedEvents(event, new HashSet<String>());
                }
            }
        } else {
            // Color not present add the color and add all the preceding and
            // following event unique identifiers. Check the color of them.
            HashSet<String> ids = new HashSet<String>();
            ids.add(event.getUniqueID());
            JHVCacheColors.setColorUsed(event.getEventRelationShip().getRelationshipColor());
            idsPerColor.put(event.getEventRelationShip().getRelationshipColor(), ids);
            addRelatedEvents(event, new HashSet<String>());
        }
    }

    private void addRelatedEvents(JHVEvent event, Set<String> handledEvents) {
        if (!handledEvents.contains(event.getUniqueID())) {
            handledEvents.add(event.getUniqueID());
            for (JHVEventRelation relation : event.getEventRelationShip().getNextEvents().values()) {
                if (relation.getTheEvent() != null) {
                    relation.getTheEvent().getEventRelationShip().setRelationshipColor(event.getEventRelationShip().getRelationshipColor());
                    idsPerColor.get(event.getEventRelationShip().getRelationshipColor()).add(relation.getTheEvent().getUniqueID());
                    addRelatedEvents(event, handledEvents);
                }
            }
            for (JHVEventRelation relation : event.getEventRelationShip().getPrecedingEvents().values()) {
                if (relation.getTheEvent() != null) {
                    relation.getTheEvent().getEventRelationShip().setRelationshipColor(event.getEventRelationShip().getRelationshipColor());
                    idsPerColor.get(event.getEventRelationShip().getRelationshipColor()).add(relation.getTheEvent().getUniqueID());
                    addRelatedEvents(event, handledEvents);
                }
            }
        }
    }

    private boolean checkFollowingAndPrecedingEvents(JHVEvent event) {
        if (checkPrecedingEvents(event, event.getEventRelationShip().getPrecedingEvents())) {
            return true;
        } else {
            return checkFollowingEvent(event, event.getEventRelationShip().getNextEvents());
        }
    }

    private boolean checkPrecedingEvents(JHVEvent event, Map<String, JHVEventRelation> precedingEvents) {
        for (String key : precedingEvents.keySet()) {
            if (idsPerColor.get(event.getEventRelationShip().getRelationshipColor()).contains(key)) {
                return true;
            } else {
                if (precedingEvents.get(key).getTheEvent() != null) {
                    return checkFollowingEvent(event, precedingEvents.get(key).getTheEvent().getEventRelationShip().getPrecedingEvents());
                }
            }
        }
        return false;
    }

    private boolean checkFollowingEvent(JHVEvent event, Map<String, JHVEventRelation> nextEvents) {
        for (String key : nextEvents.keySet()) {
            if (idsPerColor.get(event.getEventRelationShip().getRelationshipColor()).contains(key)) {
                return true;
            } else {
                if (nextEvents.get(key).getTheEvent() != null) {
                    return checkFollowingEvent(event, nextEvents.get(key).getTheEvent().getEventRelationShip().getNextEvents());
                }
            }
        }
        return false;
    }

    private void checkMissingRelations(JHVEvent event) {
        if (missingEventsInEventRelations.containsKey(event.getUniqueID())) {
            List<JHVEvent> listOfRelatedEvents = missingEventsInEventRelations.get(event.getUniqueID());
            for (JHVEvent relatedEvent : listOfRelatedEvents) {
                if (relatedEvent.getEventRelationShip().getNextEvents().containsKey(event.getUniqueID())) {
                    JHVEventRelation relation = relatedEvent.getEventRelationShip().getNextEvents().get(event.getUniqueID());
                    relation.setTheEvent(event);
                    event.getEventRelationShip().setRelationshipColor(relatedEvent.getColor());
                }
                if (relatedEvent.getEventRelationShip().getPrecedingEvents().containsKey(event.getUniqueID())) {
                    JHVEventRelation relation = relatedEvent.getEventRelationShip().getPrecedingEvents().get(event.getUniqueID());
                    relation.setTheEvent(event);
                    relatedEvent.getEventRelationShip().setRelationshipColor(event.getEventRelationShip().getRelationshipColor());
                }
                /*
                 * if
                 * (relatedEvent.getEventRelationShip().getRelationshipRules()
                 * .containsKey(event.getUniqueID())) { JHVEventRelation
                 * relation =
                 * relatedEvent.getEventRelationShip().getRelatedEventsByRule
                 * ().get(event.getUniqueID()); relation.setTheEvent(event); }
                 */
            }
            missingEventsInEventRelations.remove(event.getUniqueID());
        }
    }

    private void checkAndFixNextRelatedEvents(JHVEvent event) {
        for (JHVEventRelation er : event.getEventRelationShip().getNextEvents().values()) {
            if (er.getTheEvent() == null) {
                if (allEvents.containsKey(er.getUniqueIdentifier())) {
                    er.setTheEvent(allEvents.get(er.getUniqueIdentifier()));
                    er.getTheEvent().getEventRelationShip().setRelationshipColor(event.getEventRelationShip().getRelationshipColor());
                } else {
                    List<JHVEvent> missingRelations = new ArrayList<JHVEvent>();
                    if (missingEventsInEventRelations.containsKey(er.getUniqueIdentifier())) {
                        missingRelations = missingEventsInEventRelations.get(er.getUniqueIdentifier());
                    }
                    missingRelations.add(event);
                    missingEventsInEventRelations.put(er.getUniqueIdentifier(), missingRelations);
                }
            }
        }
    }

    private void checkAndFixPrecedingRelatedEvents(JHVEvent event) {
        for (JHVEventRelation er : event.getEventRelationShip().getPrecedingEvents().values()) {
            if (er.getTheEvent() == null) {
                if (allEvents.containsKey(er.getUniqueIdentifier())) {
                    JHVEvent relatedEvent = allEvents.get(er.getUniqueIdentifier());
                    er.setTheEvent(relatedEvent);
                    event.getEventRelationShip().setRelationshipColor(relatedEvent.getEventRelationShip().getRelationshipColor());
                } else {
                    List<JHVEvent> missingRelations = new ArrayList<JHVEvent>();
                    if (missingEventsInEventRelations.containsKey(er.getUniqueIdentifier())) {
                        missingRelations = missingEventsInEventRelations.get(er.getUniqueIdentifier());
                    }
                    missingRelations.add(event);
                    missingEventsInEventRelations.put(er.getUniqueIdentifier(), missingRelations);
                }
            }
        }
    }

    private void checkAndFixRelatedEventsByRule(JHVEvent event) {
        for (JHVEventRelation er : event.getEventRelationShip().getRelatedEventsByRule().values()) {
            if (er.getTheEvent() == null) {
                if (allEvents.containsKey(er.getUniqueIdentifier())) {
                    JHVEvent relatedEvent = allEvents.get(er.getUniqueIdentifier());
                    er.setTheEvent(relatedEvent);
                } else {
                    List<JHVEvent> missingRelations = new ArrayList<JHVEvent>();
                    if (missingEventsInEventRelations.containsKey(er.getUniqueIdentifier())) {
                        missingRelations = missingEventsInEventRelations.get(er.getUniqueIdentifier());
                    }
                    missingRelations.add(event);
                    missingEventsInEventRelations.put(er.getUniqueIdentifier(), missingRelations);
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

    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> mergeMaps(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult, Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> map) {
        for (String eventType : map.keySet()) {
            if (eventsResult.containsKey(eventType)) {
                NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>> datesPerTypeMap = map.get(eventType);
                NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>> datesPerTypeResult = eventsResult.get(eventType);
                for (Date sDate : datesPerTypeMap.keySet()) {
                    if (datesPerTypeResult.containsKey(sDate)) {
                        NavigableMap<Date, List<JHVEvent>> endDatesPerStartDateMap = datesPerTypeMap.get(sDate);
                        NavigableMap<Date, List<JHVEvent>> endDatesPerStartDateResult = datesPerTypeResult.get(sDate);
                        for (Date eDate : endDatesPerStartDateMap.keySet()) {
                            if (endDatesPerStartDateResult.containsKey(eDate)) {
                                endDatesPerStartDateResult.get(eDate).addAll(endDatesPerStartDateMap.get(eDate));
                            } else {
                                endDatesPerStartDateResult.put(eDate, endDatesPerStartDateMap.get(eDate));
                            }
                        }
                    } else {
                        datesPerTypeResult.put(sDate, datesPerTypeMap.get(sDate));
                    }
                }
            } else {
                eventsResult.put(eventType, map.get(eventType));
            }
        }
        return eventsResult;
    }
}
