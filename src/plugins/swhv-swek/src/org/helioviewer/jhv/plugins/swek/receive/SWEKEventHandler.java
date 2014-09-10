package org.helioviewer.jhv.plugins.swek.receive;

import java.util.ArrayList;
import java.util.List;

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
    public void newEventsReceived(List<JHVEvent> eventList) {

    }

}
