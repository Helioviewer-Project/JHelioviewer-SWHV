package org.helioviewer.jhv.data.datatype.event;


public abstract class AbstractJHVEventType implements JHVEventType {
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
        return ("" + getEventType() + getEventSource() + getEventProvider()).hashCode();
    }

}
