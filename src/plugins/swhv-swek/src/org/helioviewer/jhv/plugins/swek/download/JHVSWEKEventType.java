package org.helioviewer.jhv.plugins.swek.download;

import org.helioviewer.jhv.data.datatype.JHVEventType;

/**
 * JHV event type implementation for the swek.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class JHVSWEKEventType implements JHVEventType {

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
    public JHVSWEKEventType() {
        eventType = "";
        eventSource = "";
        eventProvider = "";
    }

    /**
     * Creates a JHVSWEK event type from the given event type, event source,
     * event provider.
     * 
     * @param eventType
     *            the event type
     * @param eventSource
     *            the event source
     * @param eventProvider
     *            the event provider
     */
    public JHVSWEKEventType(String eventType, String eventSource, String eventProvider) {
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
    public boolean equals(JHVEventType otherEventType) {
        return otherEventType.getEventType().equals(eventType) && otherEventType.getEventSource().equals(eventSource)
                && otherEventType.getEventProvider().equals(eventProvider);
    }

}
