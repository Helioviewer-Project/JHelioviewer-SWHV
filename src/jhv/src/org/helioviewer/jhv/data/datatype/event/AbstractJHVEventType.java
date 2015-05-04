package org.helioviewer.jhv.data.datatype.event;

import org.helioviewer.base.logging.Log;

public abstract class AbstractJHVEventType implements JHVEventType {
    @Override
    public final boolean equals(Object otherEventType) {
        if (otherEventType instanceof JHVEventType) {
            JHVEventType otherHekEvent = (JHVEventType) otherEventType;
            Log.debug("Equals return " + (otherHekEvent.getEventType().equals(getEventType()) && otherHekEvent.getEventSource().equals(getEventSource()) && otherHekEvent.getEventProvider().equals(getEventProvider())));
            return otherHekEvent.getEventType().equals(getEventType()) && otherHekEvent.getEventSource().equals(getEventSource()) && otherHekEvent.getEventProvider().equals(getEventProvider());
        } else {
            Log.debug("Equals returns false");
            return false;
        }
    }

    @Override
    public final int hashCode() {
        Log.debug("Event Type: " + getEventType());
        Log.debug("Event Source: " + getEventSource());
        Log.debug("Event Provider: " + getEventProvider());
        Log.debug(("" + getEventType() + getEventSource() + getEventProvider()).hashCode());
        return ("" + getEventType() + getEventSource() + getEventProvider()).hashCode();
    }

}
