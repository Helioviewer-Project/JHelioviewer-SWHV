package org.helioviewer.jhv.data.event;

import java.util.HashMap;

public class SWEKSupplier extends SWEKTreeModelElement {

    private final String supplierName;
    private final String db;
    private final String key;

    private final SWEKGroup group;
    private final SWEKSource source;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _supplierDisplayName, SWEKGroup _group, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        setDisplayName(_supplierDisplayName);

        group = _group;
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

    public SWEKGroup getGroup() {
        return group;
    }

    public String getKey() {
        return key;
    }

}
