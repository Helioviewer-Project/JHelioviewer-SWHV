package org.helioviewer.jhv.data.event;

import java.util.ArrayList;

public class JHVEventType {

    /** The event type */
    private final SWEKEventType eventType;

    /** The event provider */
    private final SWEKSupplier supplier;

    private JHVEventType(SWEKEventType _eventType, SWEKSupplier _supplier) {
        eventType = _eventType;
        supplier = _supplier;
    }

    private static final ArrayList<JHVEventType> evtList = new ArrayList<>();

    public static JHVEventType getJHVEventType(SWEKEventType eventType, SWEKSupplier supplier) {
        for (JHVEventType evt : evtList) {
            if (evt.supplier == supplier && evt.eventType == eventType) {
                return evt;
            }
        }
        JHVEventType toAdd = new JHVEventType(eventType, supplier);
        evtList.add(toAdd);
        return toAdd;
    }

    public SWEKEventType getEventType() {
        return eventType;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

}
