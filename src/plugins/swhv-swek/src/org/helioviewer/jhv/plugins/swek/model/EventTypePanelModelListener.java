package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;

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
    void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSupplier swekSupplier);

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
    void newEventTypeAndSourceInactive(SWEKEventType eventType, SWEKSupplier supplier);

}
