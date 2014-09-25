package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;

public interface EventTypePanelModelListener {

    /**
     * Called if the event type supplied by the swek source became active.
     * 
     * @param eventType
     *            the event type that became active
     * @param swekSource
     *            the source supplying the event type
     * @param swekSupplier
     *            the supplier that became active
     */
    public abstract void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier swekSupplier);

    /**
     * Called if the event type supplied by the swek source became inactive.
     * 
     * @param eventType
     *            the event type that became active
     * @param swekSource
     *            the source supplying the event type
     * @param supplier
     *            the supplier providing the event
     */
    public abstract void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier);
}
