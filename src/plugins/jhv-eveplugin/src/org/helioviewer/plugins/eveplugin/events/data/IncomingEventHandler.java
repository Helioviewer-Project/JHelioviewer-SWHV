package org.helioviewer.plugins.eveplugin.events.data;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.JHVEvent;

/**
 * This class handles the incoming notification of new event.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class IncomingEventHandler implements JHVEventHandler {
    /** singleton instane of the Incoming event handler */
    private static IncomingEventHandler instance;

    /** The listeners */
    private final List<IncomingEventHandlerListener> listeners;

    /**
     * Default private constructor.
     */
    private IncomingEventHandler() {
        listeners = new ArrayList<IncomingEventHandlerListener>();
    }

    /**
     * Gets the singleton instance of the IncomingEventHandler.
     * 
     * @return the singleton instance
     */
    public static IncomingEventHandler getSingletonInstance() {
        if (instance == null) {
            instance = new IncomingEventHandler();
        }
        return instance;
    }

    /**
     * Adds a new IncomingEventHandlerListener.
     * 
     * @param listener
     *            the listener to add
     */
    public void addListener(IncomingEventHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes an IncomingEventHandlerListener.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeListener(IncomingEventHandlerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void newEventsReceived(List<JHVEvent> eventList) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cacheUpdated() {
        // TODO Auto-generated method stub

    }

}
