package org.helioviewer.jhv.data.event;

import java.util.HashMap;

import org.helioviewer.jhv.data.event.filter.FilterDialog;

public class SWEKSupplier extends SWEKTreeModelElement {

    private final String supplierName;
    private final String db;
    private final String key;

    private final SWEKGroup group;
    private final SWEKSource source;

    private final boolean isCactus;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _name, SWEKGroup _group, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        name = _name.intern();

        group = _group;
        source = _source;
        db = _db;

        key = supplierName + source.getName() + db;
        suppliers.put(key, this);

        isCactus = name == "CACTus" && source.getName() == "HEK"; // interned
        if (group.containsFilter())
            filterDialog = new FilterDialog(this);
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

    public boolean isCactus() {
        return isCactus;
    }

}
