package org.helioviewer.jhv.plugins.swek.sources.comesep;

import org.helioviewer.jhv.data.datatype.event.JHVEventType;

/**
 * Defines an event type coming from the comesep source.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ComesepEventType implements JHVEventType {

    /** The event type */
    private final String eventType;

    /** The event source */
    private final String eventSource;

    /** The event provider */
    private final String eventProvider;

    /**
     * Default constructor.
     * 
     */
    public ComesepEventType() {
        eventType = "";
        eventProvider = "";
        eventSource = "";
    }

    /**
     * Creates a comesep event type with a given event type, event source, event
     * provider.
     * 
     * @param eventType
     *            the event type
     * @param eventSource
     *            the event source
     * @param eventProvider
     *            the event provider
     */
    public ComesepEventType(String eventType, String eventSource, String eventProvider) {
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventProvider = eventProvider;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getEventSource() {
        return eventSource;
    }

    @Override
    public String getEventProvider() {
        return eventProvider;
    }

    @Override
    public boolean equals(Object eventType) {
        if (eventType instanceof JHVEventType) {
            JHVEventType otherComesepEvent = (JHVEventType) eventType;
            return otherComesepEvent.getEventType().equals(this.eventType) && otherComesepEvent.getEventProvider().equals(eventProvider) && otherComesepEvent.getEventSource().equals(eventSource);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ("" + eventType + eventSource + eventProvider).hashCode();
    }

}
