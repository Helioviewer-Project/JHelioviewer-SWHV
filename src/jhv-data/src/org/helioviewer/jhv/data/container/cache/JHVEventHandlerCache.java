package org.helioviewer.jhv.data.container.cache;

import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.data.container.JHVEventHandler;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVEventHandlerCache {
    /** The singleton instance */
    public static JHVEventHandlerCache instance;

    private final Set<JHVEventHandler> allJHVeventHandlers;

    /**
     * private default constructor
     */
    private JHVEventHandlerCache() {
        allJHVeventHandlers = new HashSet<JHVEventHandler>();
    }

    /**
     * Gets the singleton instance.
     * 
     * @return The singleton instance
     */
    public static JHVEventHandlerCache getSingletonInstance() {
        if (instance == null) {
            instance = new JHVEventHandlerCache();
        }
        return instance;
    }

    /**
     * Adds a handler that wants events for a date.
     * 
     * @param handler
     *            the handler
     * @param date
     *            the date
     * @param previousRequestID
     */
    public void add(JHVEventHandler handler) {
        allJHVeventHandlers.add(handler);
    }

    /**
     * Gets all the JHVEventHandlers.
     * 
     * @return a set with all event handlers
     */
    public Set<JHVEventHandler> getAllJHVEventHandlers() {
        return allJHVeventHandlers;
    }
}
