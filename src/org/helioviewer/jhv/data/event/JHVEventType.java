package org.helioviewer.jhv.data.event;

import java.util.ArrayList;

public class JHVEventType {

    private final SWEKSupplier supplier;

    private JHVEventType(SWEKSupplier _supplier) {
        supplier = _supplier;
    }

    private static final ArrayList<JHVEventType> evtList = new ArrayList<>();

    public static JHVEventType getJHVEventType(SWEKSupplier _supplier) {
        for (JHVEventType evt : evtList) {
            if (evt.supplier == _supplier && evt.supplier.getEventType() == _supplier.getEventType()) {
                return evt;
            }
        }
        JHVEventType toAdd = new JHVEventType(_supplier);
        evtList.add(toAdd);
        return toAdd;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

}
