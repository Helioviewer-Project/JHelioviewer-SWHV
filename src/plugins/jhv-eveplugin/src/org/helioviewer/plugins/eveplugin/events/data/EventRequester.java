package org.helioviewer.plugins.eveplugin.events.data;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

/**
 * Requests events from the JHVEventContainer if the selected interval or
 * available interval changes.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventRequester implements ZoomControllerListener, JHVEventHandler {

    /** Singleton instance of the event requester */
    private static EventRequester singletonInstance;

    /** Instance of the event container */
    private final JHVEventContainer eventContainer;

    /** the selected interval */
    private Interval<Date> selectedInterval;

    /** the available interval */
    private Interval<Date> availableInterval;

    /** interval lock */
    private final Object intervalLock;

    /** The listeners */
    private final List<EventRequesterListener> listeners;

    /**
     * Private default constructor.
     * 
     */
    private EventRequester() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        availableInterval = new Interval<Date>(new Date(), new Date());
        selectedInterval = new Interval<Date>(new Date(), new Date());
        intervalLock = new Object();
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
    public void availableIntervalChanged(Interval<Date> newInterval) {
        synchronized (intervalLock) {
            availableInterval = newInterval;
            eventContainer.requestForInterval(newInterval.getStart(), newInterval.getEnd(), this);
        }
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval) {
        selectedInterval = newInterval;
        Log.info("selectedInterval : " + newInterval);
        if (!EVEState.getSingletonInstance().isMouseTimeIntervalDragging()) {
            EventQueue.invokeLater(new Runnable() {
                private Interval<Date> interval;
                private JHVEventHandler eventHandler;

                public Runnable init(Interval<Date> interval, JHVEventHandler eventHandler) {
                    this.interval = interval;
                    this.eventHandler = eventHandler;
                    return this;
                }

                @Override
                public void run() {
                    JHVEventContainer.getSingletonInstance().requestForInterval(interval.getStart(), interval.getEnd(), eventHandler);
                }

            }.init(newInterval, this));
        } else {
            Log.info("Mouse dragging");
        }
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {

    }

    @Override
    public void newEventsReceived(List<JHVEvent> eventList) {
        synchronized (intervalLock) {
            fireNewEventsReceived(eventList);
        }
    }

    @Override
    public void cacheUpdated() {
        synchronized (intervalLock) {
            eventContainer.requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), this);
        }

    }

    private void fireNewEventsReceived(List<JHVEvent> events) {
        for (EventRequesterListener l : listeners) {
            l.newEventsReceived(events);
        }
    }
}
