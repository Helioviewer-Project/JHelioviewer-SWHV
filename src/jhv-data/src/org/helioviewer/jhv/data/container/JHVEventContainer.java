package org.helioviewer.jhv.data.container;

import java.util.ArrayList;
import java.util.List;

public class JHVEventContainer {

    /** Singleton instance */
    private static JHVEventContainer singletonInstance;

    private final List<JHVEventContainerRequestHandler> handlers;

    /**
     * Private constructor.
     */
    private JHVEventContainer() {
        this.handlers = new ArrayList<JHVEventContainerRequestHandler>();
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
     * @param handler
     *            the handler to register
     */
    public void registerHandler(JHVEventContainerRequestHandler handler) {
        this.handlers.add(handler);
    }

    /**
     * Removes the JHV event container request handler.
     * 
     * @param handler
     *            the handler to remove
     */
    public void removeHandler(JHVEventContainerRequestHandler handler) {
        this.handlers.remove(handler);
    }
}
