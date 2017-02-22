package org.helioviewer.jhv.plugins.swek.sources.hek;

import org.jetbrains.annotations.NotNull;

// Factory creates a HEKEvent from a String
class HEKEventFactory {
    /**
     * Creates a HEK event from a String.
     *
     * @param eventType
     *            The string description of the event
     * @return The HEKEvent if known or the unknown hek event.
     */
    @NotNull
    public static HEKEventEnum getHEKEvent(String eventType) {
        for (HEKEventEnum event : HEKEventEnum.values()) {
            if (event.getSWEKEventName().equals(eventType)) {
                return event;
            }
        }
        return HEKEventEnum.UNKNOWN;
    }

}
