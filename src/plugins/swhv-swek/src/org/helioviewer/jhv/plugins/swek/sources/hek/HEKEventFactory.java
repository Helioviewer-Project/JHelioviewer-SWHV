package org.helioviewer.jhv.plugins.swek.sources.hek;

/**
 * Factory creates a HEKEvent from a String.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class HEKEventFactory {
    /**
     * Creates a HEK event from a String.
     * 
     * @param eventType
     *            The string description of the event
     * @return The HEKEvent if known or the unknown hek event.
     */
    public static HEKEventEnum getHEKEvent(String eventType) {
        HEKEventEnum unknown = HEKEventEnum.UNKNOWN;
        Object[] possibleEvents = unknown.getDeclaringClass().getEnumConstants();
        for (Object hekEvent : possibleEvents) {
            if (hekEvent instanceof HEKEventEnum) {
                HEKEventEnum event = (HEKEventEnum) hekEvent;
                if (event.getSWEKEventName().equals(eventType)) {
                    return event;
                }
            }
        }

        return HEKEventEnum.UNKNOWN;
    }

}
