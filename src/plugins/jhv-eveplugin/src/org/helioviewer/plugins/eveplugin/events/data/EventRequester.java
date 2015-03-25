package org.helioviewer.plugins.eveplugin.events.data;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
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
    public void availableIntervalChanged(final Interval<Date> newInterval) {
        availableInterval = newInterval;
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                eventContainer.requestForInterval(newInterval.getStart(), newInterval.getEnd(), EventRequester.this);
            }
        });
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
        selectedInterval = newInterval;
        // Log.info("selectedInterval : " + newInterval);
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
                    // long start = System.currentTimeMillis();
                    JHVEventContainer.getSingletonInstance().requestForInterval(interval.getStart(), interval.getEnd(), eventHandler);
                    // Log.debug("selectedIntervalChanged" +
                    // (System.currentTimeMillis() - start));
                }

            }.init(newInterval, this));
        } else {
            // Log.info("Mouse dragging");
        }
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {

    }

    @Override
    public void newEventsReceived(final Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                fireNewEventsReceived(eventList);
            }

        });

    }

    @Override
    public void cacheUpdated() {
        EventQueue.invokeLater((new Runnable() {

            private Interval<Date> selectedInterval;
            private JHVEventHandler eventHandler;

            public Runnable init(Interval<Date> selectedInterval, JHVEventHandler eventHandler) {
                this.selectedInterval = selectedInterval;
                this.eventHandler = eventHandler;
                return this;
            }

            @Override
            public void run() {
                eventContainer.requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), eventHandler);
            }

        }).init(selectedInterval, this));

    }

    private void fireNewEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events) {
        for (EventRequesterListener l : listeners) {
            l.newEventsReceived(events);
        }
    }
}
