package org.helioviewer.jhv.data.event;

import java.util.HashMap;

public class SWEKSupplier extends SWEKTreeModelElement {

    private final String supplierName;
    private final String db;
    private final String key;

    private final SWEKEventType eventType;
    private final SWEKSource source;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _supplierDisplayName, SWEKEventType _eventType, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        setDisplayName(_supplierDisplayName);

        eventType = _eventType;
        source = _source;
        db = _db;

        key = supplierName + source.getSourceName() + db;
        suppliers.put(key, this);
    }

    public static SWEKSupplier getSupplier(String name) {
        return suppliers.get(name);
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getDatabaseName() {
        return db;
    }

    public SWEKSource getSource() {
        return source;
    }

    public SWEKEventType getEventType() {
        return eventType;
    }

    public String getKey() {
        return key;
    }

}
