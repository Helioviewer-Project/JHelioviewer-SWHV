package org.helioviewer.jhv.data.container;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.container.cache.JHVEventHandlerCache;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

public class JHVEventContainer {

    /** Singleton instance */
    private static JHVEventContainer singletonInstance;

    /** the event cache */
    private final JHVEventCache eventCache;

    /** the event handler cache */
    private final JHVEventHandlerCache eventHandlerCache;

    private JHVEventContainerRequestHandler incomingRequestManager;

    private static final double factor = 0.2;

    private static JHVRelatedEvents lastHighlighted = null;

    /**
     * Private constructor.
     */
    private JHVEventContainer() {
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
        // Log.debug("Request for interval : [" + startDate + "," + endDate + "]");
        // Logger.getLogger(JHVEventContainer.class.getName()).info("handler : " + handler);
        if (startDate != null && endDate != null) {
            long deltaT = endDate.getTime() - startDate.getTime();
            Date newStartDate = new Date((long) (startDate.getTime() - deltaT * factor));
            Date newEndDate = new Date((long) (endDate.getTime() + deltaT * factor));
            // Log.debug("new Interval : [" + newStartDate + "," + newEndDate + "]");
            eventHandlerCache.add(handler);
            JHVEventCacheResult result = eventCache.get(startDate, endDate, newStartDate, newEndDate);
            Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = result.getAvailableEvents();
            // AssociationsPrinter.print(events);
            handler.newEventsReceived(events);
            for (JHVEventType eventType : result.getMissingIntervals().keySet()) {
                List<Interval<Date>> missingList = result.getMissingIntervals().get(eventType);
                for (Interval<Date> missing : missingList) {
                    requestEvents(eventType, missing.getStart(), missing.getEnd());
                }
            }
        }
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
        incomingRequestManager.handleRequestForInterval(eventType, startDate, endDate);
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

    public static void highlight(JHVRelatedEvents event) {
        if (event == lastHighlighted)
            return;
        if (event != null) {
            event.highlight(true);

        }
        if (lastHighlighted != null) {
            lastHighlighted.highlight(false);
        }
        lastHighlighted = event;
    }

    public void registerHandler(JHVEventContainerRequestHandler incomingRequestManager) {
        this.incomingRequestManager = incomingRequestManager;
    }

}
