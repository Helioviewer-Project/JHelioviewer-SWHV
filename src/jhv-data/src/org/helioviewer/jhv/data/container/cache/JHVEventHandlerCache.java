package org.helioviewer.jhv.data.container.cache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.util.DateUtil;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventHandlerCache {
    /** The singleton instance */
    public static JHVEventHandlerCache instance;

    /** The event handlers that want event JHVevents a date */
    private final Map<Date, Set<JHVEventHandler>> interestInDate;

    /** Handlers that want events within an interval */
    private final Map<Date, Map<Date, Set<JHVEventHandler>>> interestInInterval;

    private final Map<JHVEventHandler, Long> allJHVeventHandlers;

    /**
     * private default constructor
     */
    private JHVEventHandlerCache() {
        interestInDate = new HashMap<Date, Set<JHVEventHandler>>();
        interestInInterval = new HashMap<Date, Map<Date, Set<JHVEventHandler>>>();
        allJHVeventHandlers = new HashMap<JHVEventHandler, Long>();
    }

    /**
     * Gets the singleton instance.
     * 
     * @return The singleton instance
     */
    public static JHVEventHandlerCache getSingletonInstance() {
        if (instance == null) {
            instance = new JHVEventHandlerCache();
        }
        return instance;
    }

    /**
     * Adds a handler that wants events for a date.
     * 
     * @param handler
     *            the handler
     * @param date
     *            the date
     * @param previousRequestID
     */
    public Long add(JHVEventHandler handler, Date date, Long requestID) {
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            if (date != null && handler != null) {
                Date roundedDate = DateUtil.getCurrentDate(date);
                Set<JHVEventHandler> tempSet = new HashSet<JHVEventHandler>();
                if (interestInDate.containsKey(roundedDate)) {
                    tempSet = interestInDate.get(roundedDate);
                }
                tempSet.add(handler);
                interestInDate.put(roundedDate, tempSet);
                if (allJHVeventHandlers.containsKey(handler)) {
                    Long previousRequestID = allJHVeventHandlers.get(handler);
                    allJHVeventHandlers.put(handler, requestID);
                    return previousRequestID;
                } else {
                    return null;
                }
            } else {
                // This should be logged, but the log system is part of the
                // complete program. Should be stripped of.
                System.err.println("The date or handler was null");
                return null;
            }
        }
    }

    /**
     * Gets all the JHVEventHandlers.
     * 
     * @return a set with all event handlers
     */
    public Set<JHVEventHandler> getAllJHVEventHandlers() {
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            return allJHVeventHandlers.keySet();
        }
    }

    /**
     * Adds a handler that want events over an interval
     * 
     * @param handler
     *            the handler
     * @param startDate
     *            the start date of the interval
     * @param endDate
     *            the end date of the interval
     * @param requestID
     * @return
     */
    public Long add(JHVEventHandler handler, Date startDate, Date endDate, Long requestID) {
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            Date roundedStartDate = DateUtil.getCurrentDate(startDate);
            Date roundedEndDate = DateUtil.getNextDate(endDate);
            Map<Date, Set<JHVEventHandler>> eventHandlerEnd = new HashMap<Date, Set<JHVEventHandler>>();
            Set<JHVEventHandler> eventHandlers = new HashSet<JHVEventHandler>();
            if (interestInInterval.containsKey(roundedStartDate)) {
                eventHandlerEnd = interestInInterval.get(roundedStartDate);
                if (eventHandlerEnd.containsKey(roundedEndDate)) {
                    eventHandlers = eventHandlerEnd.get(roundedEndDate);
                }
            }
            eventHandlers.add(handler);
            eventHandlerEnd.put(roundedEndDate, eventHandlers);
            interestInInterval.put(roundedStartDate, eventHandlerEnd);
            if (allJHVeventHandlers.containsKey(handler)) {
                Long previousRequestID = allJHVeventHandlers.get(handler);
                allJHVeventHandlers.put(handler, requestID);
                return previousRequestID;
            } else {
                allJHVeventHandlers.put(handler, requestID);
                return null;
            }
        }
    }

    /**
     * Gets all the handlers that want events for a date.
     * 
     * @param date
     *            the date for which the handlers want events
     * @return a set containing all the handlers
     */
    public List<JHVEventHandler> getJHVEventHandlersForDate(Date date) {
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            List<JHVEventHandler> handlers = new ArrayList<JHVEventHandler>();
            Date roundedDate = DateUtil.getCurrentDate(date);
            handlers.addAll(getEventHandlersFromDate(roundedDate));
            handlers.addAll(getEventHandlersFromInterval(roundedDate));
            return handlers;
        }
    }

    /**
     * Gets the handlers from the interestedInDate.
     * 
     * @param roundedDate
     *            The date rounded on the day
     * @return the set of handlers interested in the date
     */
    private Set<JHVEventHandler> getEventHandlersFromDate(Date roundedDate) {
        Set<JHVEventHandler> handlers = new HashSet<JHVEventHandler>();
        if (interestInDate.containsKey(roundedDate)) {
            handlers.addAll(interestInDate.get(roundedDate));
        }
        return handlers;
    }

    /**
     * Gets the handlers from the interestedInInterval.
     * 
     * @param roundedDate
     *            The date rounded on the day
     * @return the set of handlers interested in the date
     */
    private Set<JHVEventHandler> getEventHandlersFromInterval(Date roundedDate) {
        Set<JHVEventHandler> handlers = new HashSet<JHVEventHandler>();
        Calendar reference = Calendar.getInstance();
        reference.setTime(roundedDate);
        for (Date startDate : interestInInterval.keySet()) {
            Calendar startDateCal = Calendar.getInstance();
            startDateCal.setTime(startDate);
            if (startDateCal.compareTo(reference) <= 0) {
                Map<Date, Set<JHVEventHandler>> eventHandlersEnd = interestInInterval.get(startDate);
                for (Date endDate : eventHandlersEnd.keySet()) {
                    Calendar endDateCal = Calendar.getInstance();
                    endDateCal.setTime(endDate);
                    if (endDateCal.compareTo(reference) >= 0) {
                        handlers.addAll(eventHandlersEnd.get(endDate));
                    }
                }
            }
        }
        return handlers;
    }
}
