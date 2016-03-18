package org.helioviewer.jhv.data.datatype.event;

public class JHVEventType {

    /** The event type */
    private final String eventType;

    /** The event source */
    private final String eventSource;

    /** The event provider */
    private final String eventProvider;

    /**
     * Default constructor.
     */
    public JHVEventType() {
        eventType = "";
        eventSource = "";
        eventProvider = "";
    }

    public JHVEventType(String eventType, String eventSource, String eventProvider) {
        this.eventType = eventType;
        this.eventSource = eventSource;
        this.eventProvider = eventProvider;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventSource() {
        return eventSource;
    }

    public String getEventProvider() {
        return eventProvider;
    }

    @Override
    public final boolean equals(Object otherEventType) {
        if (otherEventType instanceof JHVEventType) {
            JHVEventType otherHekEvent = (JHVEventType) otherEventType;
            return otherHekEvent.getEventType().equals(getEventType()) && otherHekEvent.getEventSource().equals(getEventSource()) && otherHekEvent.getEventProvider().equals(getEventProvider());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return (getEventType() + getEventSource() + getEventProvider()).hashCode();
    }

}
