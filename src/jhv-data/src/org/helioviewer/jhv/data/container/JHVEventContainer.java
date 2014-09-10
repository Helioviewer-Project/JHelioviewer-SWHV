package org.helioviewer.jhv.data.container;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventHandlerCache;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

public class JHVEventContainer {

    /** Singleton instance */
    private static JHVEventContainer singletonInstance;

    /** The handlers of requests */
    private final List<JHVEventContainerRequestHandler> requestHandlers;

    /** the event cache */
    private final JHVEventCache eventCache;

    /** the event handler cache */
    private final JHVEventHandlerCache eventHandlerCache;

    /**
     * Private constructor.
     */
    private JHVEventContainer() {
        requestHandlers = new ArrayList<JHVEventContainerRequestHandler>();
        eventHandlerCache = JHVEventHandlerCache.getSingletonInstance();
        eventCache = JHVEventCache.getSingletonInstance();
    }

    /**
     * Gets the singleton instance of the JHVEventContainer
     * 
     * @return the singleton instance
     */
    public static JHVEventContainer getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new JHVEventContainer();
        }
        return singletonInstance;
    }

    /**
     * Register a JHV event container request handler.
     * 
     * 
     * @param handler
     *            the handler to register
     */
    public void registerHandler(JHVEventContainerRequestHandler handler) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            requestHandlers.add(handler);
        }
    }

    /**
     * Removes the JHV event container request handler.
     * 
     * @param handler
     *            the handler to remove
     */
    public void removeHandler(JHVEventContainerRequestHandler handler) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            requestHandlers.remove(handler);
        }
    }

    /**
     * Request the JHVEventContainer for events from a specific date. The events
     * will be send to the given handler. Events already available will directly
     * be send to the handler. Events becoming available will also be send to
     * the handler in the future.
     * 
     * @param date
     *            the date to send events for
     * @param handler
     *            the handler to send events to
     */
    public void requestForDate(Date date, JHVEventHandler handler) {
        synchronized (JHVEventContainerLocks.dateLock) {
            eventHandlerCache.add(handler, date);
            List<JHVEvent> events = eventCache.get(date);
            handler.newEventsReceived(events);
            requestEvents(date);
        }
    }

    /**
     * Request the JHVEventContainer for events from a specific list of dates.
     * The events will be send to the given handler. Events already available
     * will directly be send to the handler. Events becoming available will also
     * be send to the handler in the future.
     * 
     * @param dateList
     *            the list of dates to send events for
     * @param handler
     *            the handler to send events to
     */
    public void requestForDateList(List<Date> dateList, JHVEventHandler handler) {
        synchronized (JHVEventContainerLocks.dateLock) {
            for (Date date : dateList) {
                requestForDate(date, handler);
            }
        }
    }

    /**
     * Request the JHVEventContainer for events from a specific time interval.
     * The events will be send to the given handler. Events already available
     * will directly be send to the handler. Events becoming available will also
     * be send to the handler in the future.
     * 
     * @param startDate
     *            the start date of the interval
     * @param endDate
     *            the end date of the interval
     * @param handler
     *            the handler
     */
    public void requestForInterval(Date startDate, Date endDate, JHVEventHandler handler) {
        synchronized (JHVEventContainerLocks.intervalLock) {
            eventHandlerCache.add(handler, startDate, endDate);
            List<JHVEvent> events = eventCache.get(startDate, endDate);
            handler.newEventsReceived(events);
            requestEvents(startDate, endDate);
        }
    }

    /**
     * Request data from the request handlers for a date.
     * 
     * @param date
     *            the date for which to request the data
     */
    private void requestEvents(Date date) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            for (JHVEventContainerRequestHandler handler : requestHandlers) {
                handler.handleRequestForDate(date);
            }
        }
    }

    /**
     * Request data from the request handlers over an interval.
     * 
     * @param startDate
     *            the start of the interval
     * @param endDate
     *            the end of the interval
     */
    private void requestEvents(Date startDate, Date endDate) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            for (JHVEventContainerRequestHandler handler : requestHandlers) {
                handler.handleRequestForInterval(startDate, endDate);
            }
        }
    }
}
