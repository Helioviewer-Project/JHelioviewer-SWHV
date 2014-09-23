package org.helioviewer.jhv.data.datatype;

/**
 * Describes the type of an event.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface JHVEventType {
    /**
     * Gets the event type.
     * 
     * @return the event type
     */
    public abstract String getEventType();

    /**
     * Gets the event source.
     * 
     * @return the event source
     */
    public abstract String getEventSource();

    /**
     * Gets the event provider.
     * 
     * @return the provider
     */
    public abstract String getEventProvider();

    /**
     * Check if the given event type is equal to this event type.
     * 
     * @param eventType
     *            the event type to compare with
     * @return true if both event types are equal, false if not
     */
    public abstract boolean equals(JHVEventType eventType);
}
