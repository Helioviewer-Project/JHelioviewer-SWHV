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
    public abstract String getEvenProvider();
}
