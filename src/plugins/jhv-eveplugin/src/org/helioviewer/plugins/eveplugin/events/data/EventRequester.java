package org.helioviewer.plugins.eveplugin.events.data;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
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
public class EventRequester implements ZoomControllerListener {

    /** Singleton instance of the event requester */
    private static EventRequester singletonInstance;

    /** Instance of the event container */
    private final JHVEventContainer eventContainer;

    /** Instance of the incoming event handler */
    private final IncomingEventHandler incomingEventHandler;

    /**
     * Private default constructor.
     * 
     */
    private EventRequester() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        incomingEventHandler = IncomingEventHandler.getSingletonInstance();
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
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

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        eventContainer.requestForInterval(newInterval.getStart(), newInterval.getEnd(), incomingEventHandler);
    }

    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval) {
        eventContainer.requestForInterval(newInterval.getStart(), newInterval.getEnd(), incomingEventHandler);
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
    }

}
