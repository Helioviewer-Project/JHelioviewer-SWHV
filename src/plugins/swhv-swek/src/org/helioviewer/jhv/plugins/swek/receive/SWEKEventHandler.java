package org.helioviewer.jhv.plugins.swek.receive;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.JHVEvent;

public class SWEKEventHandler implements JHVEventHandler {
    /** Singleton instance of the SWEKEventHandler */
    private static SWEKEventHandler instance;

    /** the listeners */
    private final List<SWEKEventHandlerListener> listeners;

    /**
     * Private default constructor
     */
    private SWEKEventHandler() {
        listeners = new ArrayList<SWEKEventHandlerListener>();
    }

    /**
     * Gets the singleton instance of the SWEKEventHandler
     * 
     * @return the singleton instance
     */
    public static SWEKEventHandler getSingletonInstace() {
        if (instance == null) {
            instance = new SWEKEventHandler();
        }
        return instance;
    }

    /**
     * Adds a SWEKEventHandlerListener to the SWEKEventHandler
     * 
     * @param listener
     *            the listener to add
     */
    public void addSWEKEventHandlerListener(SWEKEventHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the SWEKEventHandlerListener from the SWEKEventHandler
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeSWEKEventHandkerListener(SWEKEventHandlerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
        Log.info("New events received.");
        /*
         * for (JHVEvent event : eventList) { Log.info(event.getName() + " " +
         * event.getDisplayName() + " " + event.getStartDate() + "-" +
         * event.getEndDate()); }
         */
        for (String eventType : eventList.keySet()) {
            for (Date sDate : eventList.get(eventType).keySet()) {
                for (Date eDate : eventList.get(eventType).get(sDate).keySet()) {
                    for (JHVEvent event : eventList.get(eventType).get(sDate).get(eDate)) {
                        Log.info(event.getName() + " " + event.getDisplayName() + " " + event.getStartDate() + "-" + event.getEndDate());
                    }
                }
            }
        }
    }

    @Override
    public void cacheUpdated() {
        Log.info("New Cache update received");
    }

}
