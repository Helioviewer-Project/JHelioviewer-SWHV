package org.helioviewer.plugins.eveplugin.events.data;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.TimingListener;

/**
 * Requests events from the JHVEventContainer if the selected interval or
 * available interval changes.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventRequester implements TimingListener, JHVEventHandler {

    /** Singleton instance of the event requester */
    private static EventRequester singletonInstance;

    /** Instance of the event container */
    private final JHVEventContainer eventContainer;

    /** The listeners */
    private final List<EventRequesterListener> listeners;

    /**
     * Private default constructor.
     *
     */
    private EventRequester() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        DrawController.getSingletonInstance().addTimingListener(this);
        listeners = new ArrayList<EventRequesterListener>();
    }

    /**
     * Gets the singleton instance of the EventRequester.
     *
     * @return The singleton instance
     */
    public static EventRequester getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new EventRequester();
        }
        return singletonInstance;
    }

    /**
     * Adds a new IncomingEventHandlerListener.
     *
     * @param listener
     *            the listener to add
     */
    public void addListener(EventRequesterListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes an IncomingEventHandlerListener.
     *
     * @param listener
     *            the listener to remove
     */
    public void removeListener(EventRequesterListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void availableIntervalChanged() {
        Interval<Date> availableInterval = DrawController.getSingletonInstance().getAvailableInterval();
        eventContainer.requestForInterval(availableInterval.getStart(), availableInterval.getEnd(), EventRequester.this);
    }

    @Override
    public void selectedIntervalChanged() {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called by other thread than event queue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(666);
        }
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        JHVEventContainer.getSingletonInstance().requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), this);
    }

    @Override
    public void newEventsReceived(final Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called by other thread than event queue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(666);
        }
        fireNewEventsReceived(eventList);
    }

    @Override
    public void cacheUpdated() {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called by other thread than event queue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(666);
        }
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        eventContainer.requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), this);
    }

    private void fireNewEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events) {
        for (EventRequesterListener l : listeners) {
            l.newEventsReceived(events);
        }
    }
}
