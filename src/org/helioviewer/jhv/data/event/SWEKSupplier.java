package org.helioviewer.jhv.data.event;

import java.util.HashMap;

public class SWEKSupplier extends SWEKTreeModelElement {

    private final String supplierName;
    private final String db;

    private final SWEKEventType eventType;
    private final SWEKSource source;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _supplierDisplayName, SWEKEventType _eventType, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        setDisplayName(_supplierDisplayName);

        eventType = _eventType;
        source = _source;
        db = _db;

        String key = supplierName + source.getSourceName() + db;
        suppliers.put(key, this);
    }

    public static SWEKSupplier getSupplier(String supplierNameKey) {
        return suppliers.get(supplierNameKey);
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

    public String getSupplierKey() {
        return supplierName + source.getSourceName() + db;
    }

}
