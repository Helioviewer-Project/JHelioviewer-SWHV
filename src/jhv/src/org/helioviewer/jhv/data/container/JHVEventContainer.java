package org.helioviewer.jhv.data.container;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.helioviewer.base.cache.RequestCache;
import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.container.cache.JHVEventHandlerCache;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

public class JHVEventContainer {

    /** Singleton instance */
    private static JHVEventContainer singletonInstance;

    /** The handlers of requests */
    private final List<JHVEventContainerRequestHandler> requestHandlers;

    /** the event cache */
    private final JHVEventCache eventCache;

    /** the event handler cache */
    private final JHVEventHandlerCache eventHandlerCache;

    private final Map<JHVEventType, RequestCache> missingIntervals;

    private final Timer eventTimer;

    /**
     * Private constructor.
     */
    private JHVEventContainer() {
        requestHandlers = new ArrayList<JHVEventContainerRequestHandler>();
        eventHandlerCache = JHVEventHandlerCache.getSingletonInstance();
        eventCache = JHVEventCache.getSingletonInstance();
        missingIntervals = new HashMap<JHVEventType, RequestCache>();
        eventTimer = new Timer("SWEK download missing");
        eventTimer.schedule(new EventDownloadTask(), 0, 5000);
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
        requestHandlers.add(handler);
    }

    /**
     * Removes the JHV event container request handler.
     *
     * @param handler
     *            the handler to remove
     */
    public void removeHandler(JHVEventContainerRequestHandler handler) {
        requestHandlers.remove(handler);
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
    public void requestForDate(final Date date, final JHVEventHandler handler) {
        eventHandlerCache.add(handler);
        JHVEventCacheResult result = eventCache.get(date);
        Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events = result.getAvailableEvents();
        handler.newEventsReceived(events);
        for (Date missingDate : result.getMissingDates()) {
            requestEvents(missingDate);
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
    public void requestForDateList(final List<Date> dateList, final JHVEventHandler handler) {
        for (Date date : dateList) {
            requestForDate(date, handler);
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
    public void requestForInterval(final Date startDate, final Date endDate, final JHVEventHandler handler) {
        // Logger.getLogger(JHVEventContainer.class.getName()).info("Request for interval : ["
        // + startDate + "," + endDate + "]");
        // Logger.getLogger(JHVEventContainer.class.getName()).info("handler : "
        // + handler);
        if (startDate != null && endDate != null) {
            eventHandlerCache.add(handler);
            JHVEventCacheResult result = eventCache.get(startDate, endDate);
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events = result.getAvailableEvents();
            // AssociationsPrinter.print(events);
            handler.newEventsReceived(events);
            for (JHVEventType eventType : result.getMissingIntervals().keySet()) {
                // Log.debug("Missing interval: " + missing);
                List<Interval<Date>> missingList = result.getMissingIntervals().get(eventType);
                for (Interval<Date> missing : missingList) {
                    // requestEvents(eventType, missing.getStart(),
                    // missing.getEnd());
                    RequestCache rc = new RequestCache();
                    if (missingIntervals.containsKey(eventType)) {
                        rc = missingIntervals.get(eventType);
                    }
                    rc.adaptRequestCache(missing.getStart(), missing.getEnd());
                    missingIntervals.put(eventType, rc);
                }
            }
        }
    }

    /**
     * Add an event to the event cache.
     *
     * @param event
     *            the event to add to the event cache
     */
    public void addEvent(final JHVEvent event) {
        eventCache.add(event);
    }

    /**
     * Indicates to the JHVEventContainer that a download was finished. This
     * must be called the event request handlers in order to propagate the
     * downloaded events to the event handlers.
     */
    public void finishedDownload() {
        fireEventCacheChanged();
    }

    /**
     * Removes the events of the given eventType from the event cache.
     *
     * @param eventType
     *            the event type to remove from the cache.
     */
    public void removeEvents(final JHVEventType eventType) {
        eventCache.removeEventType(eventType);
        fireEventCacheChanged();
    }

    /**
     * Request data from the request handlers for a date.
     *
     * @param date
     *            the date for which to request the data
     */
    private void requestEvents(Date date) {
        for (JHVEventContainerRequestHandler handler : requestHandlers) {
            handler.handleRequestForDate(date);
        }

    }

    /**
     * Request data from the request handlers over an interval.
     *
     * @param eventType
     *
     * @param startDate
     *            the start of the interval
     * @param endDate
     *            the end of the interval
     */
    private void requestEvents(JHVEventType eventType, Date startDate, Date endDate) {
        for (JHVEventContainerRequestHandler handler : requestHandlers) {
            handler.handleRequestForInterval(eventType, startDate, endDate);
        }
    }

    /**
     * Notify the interested JHVEventhandler of about the cache that was
     * changed.
     *
     * @param date
     *            the date for which the cache was changed.
     */
    private void fireEventCacheChanged() {
        Set<JHVEventHandler> handlers = eventHandlerCache.getAllJHVEventHandlers();
        for (JHVEventHandler handler : handlers) {
            handler.cacheUpdated();
        }
    }

    /**
     * Gets all the intervals that were requested.
     *
     * @return A list with intervals
     */
    public Collection<Interval<Date>> getAllRequestIntervals(JHVEventType eventType) {
        return JHVEventCache.getSingletonInstance().getAllRequestIntervals(eventType);
    }

    public void intervalsNotDownloaded(JHVEventType eventType, Interval<Date> interval) {
        JHVEventCache.getSingletonInstance().removeRequestedIntervals(eventType, interval);
    }

    public void eventTypeActivated(JHVEventType eventType) {
        eventCache.eventTypeActivated(eventType);
        fireEventCacheChanged();
    }

    public class EventDownloadTask extends TimerTask {

        @Override
        public void run() {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (!missingIntervals.isEmpty()) {
                        for (JHVEventType evt : missingIntervals.keySet()) {
                            for (Interval<Date> interv : missingIntervals.get(evt).getAllRequestIntervals()) {
                                requestEvents(evt, interv.getStart(), interv.getEnd());
                            }
                        }
                    }
                    missingIntervals.clear();
                }

            });
        }

    }
}
