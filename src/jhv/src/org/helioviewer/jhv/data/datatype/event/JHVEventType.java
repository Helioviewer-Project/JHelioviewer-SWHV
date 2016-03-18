package org.helioviewer.jhv.data.datatype.event;

public class JHVEventType {

    /** The event type */
    private final String eventType;

    /** The event provider */
    private final String eventProvider;

    public JHVEventType(String eventType, String eventProvider) {
        this.eventType = eventType;
        this.eventProvider = eventProvider;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventProvider() {
        return eventProvider;
    }

    @Override
    public String toString() {
        return eventProvider + " " + eventType;
    }

    @Override
    public final boolean equals(Object otherEventType) {
        if (otherEventType instanceof JHVEventType) {
            JHVEventType otherHekEvent = (JHVEventType) otherEventType;
            return otherHekEvent.getEventType().equals(getEventType()) && otherHekEvent.getEventProvider().equals(getEventProvider());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return (getEventType() + getEventProvider()).hashCode();
    }

}
