package org.helioviewer.jhv.data.container.cache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.container.util.DateUtil;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

public class JHVEventCache {
    /** signleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    private final Map<Date, Map<Date, List<JHVEvent>>> events;

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<Date, Map<Date, List<JHVEvent>>>();
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
            Date startDate = DateUtil.getCurrentDate(event.getStartDate());
            Date endDate = DateUtil.getNextDate(event.getEndDate());
            addToList(startDate, endDate, event);
        }
    }

    /**
     * Gets the events containing the given date.
     * 
     * @param date
     *            The date in which the event should have happened
     * @return the list of events happened on the given date
     */
    public List<JHVEvent> get(Date date) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            List<JHVEvent> eventsResult = new ArrayList<JHVEvent>();
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
                            eventsResult.addAll(eventOnEndTime.get(eDate));
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
    public List<JHVEvent> get(List<Date> dates) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            List<JHVEvent> events = new ArrayList<JHVEvent>();
            for (Date date : dates) {
                events.addAll(get(date));
            }
            return events;
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
    public List<JHVEvent> get(Date startDate, Date endDate) {
        synchronized (JHVEventContainerLocks.cacheLock) {
            List<JHVEvent> eventsResult = new ArrayList<JHVEvent>();
            Calendar intervalS = Calendar.getInstance();
            intervalS.setTime(startDate);
            Calendar intervalE = Calendar.getInstance();
            intervalE.setTime(endDate);
            for (Date sDate : events.keySet()) {
                Calendar tempS = Calendar.getInstance();
                tempS.setTime(sDate);
                if (intervalS.compareTo(tempS) <= 0 && intervalE.compareTo(tempS) >= 0) {
                    for (List<JHVEvent> tempEvent : events.get(sDate).values()) {
                        eventsResult.addAll(tempEvent);
                    }
                }
            }
            return eventsResult;
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
}
