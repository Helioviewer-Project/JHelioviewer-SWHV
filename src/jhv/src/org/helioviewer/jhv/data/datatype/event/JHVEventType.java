package org.helioviewer.jhv.data.datatype.event;

import java.util.ArrayList;

public class JHVEventType {

    /** The event type */
    private final SWEKEventType eventType;

    /** The event provider */
    private final SWEKSupplier supplier;

    private JHVEventType(SWEKEventType eventType, SWEKSupplier supplier) {
        this.eventType = eventType;
        this.supplier = supplier;
    }

    private static final ArrayList<JHVEventType> evtList = new ArrayList<JHVEventType>();

    public static JHVEventType getJHVEventType(SWEKEventType eventType, SWEKSupplier supplier) {
        for (JHVEventType evt : evtList) {
            if (evt.supplier == supplier && evt.getEventType() == eventType) {
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
