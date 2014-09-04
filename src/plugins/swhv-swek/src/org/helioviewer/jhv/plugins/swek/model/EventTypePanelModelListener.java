package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;

public interface EventTypePanelModelListener {

    /**
     * Called if the event type supplied by the swek source became active.
     * 
     * @param eventType
     *            the event type that became active
     * @param swekSource
     *            the source supplying the event type
     */
    public abstract void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource);

    /**
     * Called if the event type supplied by the swek source became inactive.
     * 
     * @param eventType
     *            the event type that became active
     * @param swekSource
     *            the source supplying the event type
     */
    public abstract void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource);
}
