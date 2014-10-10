package org.helioviewer.jhv.data.container.cache;

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
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

public class JHVEventCache {
    /** singleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    private final Map<Date, Map<Date, List<JHVEvent>>> events;

    /**  */
    private final Set<String> eventIDs;

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<Date, Map<Date, List<JHVEvent>>>();
        eventIDs = new HashSet<String>();
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
                Date startDate = DateUtil.getCurrentDate(event.getStartDate());
                Date endDate = DateUtil.getNextDate(event.getEndDate());
                addToList(startDate, endDate, event);
                eventIDs.add(event.getUniqueID());
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
                        }
                    }
                    eventList.removeAll(deleteList);
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
    private void addEventsToResult(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult,
            List<JHVEvent> tempEvents) {
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

    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> mergeMaps(
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventsResult,
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> map) {
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
