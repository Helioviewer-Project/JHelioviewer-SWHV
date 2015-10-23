package org.helioviewer.jhv.data.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.helioviewer.jhv.base.interval.Interval;
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

    private static final double factor = 0.2;

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
        // Log.debug("Request for interval : [" + startDate + "," + endDate +
        // "]");
        // Logger.getLogger(JHVEventContainer.class.getName()).info("handler : "
        // + handler);
        if (startDate != null && endDate != null) {
            long deltaT = endDate.getTime() - startDate.getTime();
            Date newStartDate = new Date((long) (startDate.getTime() - deltaT * factor));
            Date newEndDate = new Date((long) (endDate.getTime() + deltaT * factor));
            // Log.debug("new Interval : [" + newStartDate + "," + newEndDate +
            // "]");
            eventHandlerCache.add(handler);
            JHVEventCacheResult result = eventCache.get(startDate, endDate, newStartDate, newEndDate);
            Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events = result.getAvailableEvents();
            // AssociationsPrinter.print(events);
            handler.newEventsReceived(events);
            for (JHVEventType eventType : result.getMissingIntervals().keySet()) {
                List<Interval<Date>> missingList = result.getMissingIntervals().get(eventType);
                for (Interval<Date> missing : missingList) {
                    // Log.debug("Missing interval: " + missing);
                    requestEvents(eventType, missing.getStart(), missing.getEnd());
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
     *
     * @param partially
     */
    public void finishedDownload(boolean partially) {
        fireEventCacheChanged();
    }

    /**
     * Removes the events of the given eventType from the event cache.
     *
     * @param eventType
     *            the event type to remove from the cache.
     */
    public void removeEvents(final JHVEventType eventType, boolean keepActive) {
        eventCache.removeEventType(eventType, keepActive);
        fireEventCacheChanged();
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
        // Log.debug("Request for events: " + requestHandlers.size());
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

}
