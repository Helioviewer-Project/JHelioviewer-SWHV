package org.helioviewer.jhv.event;

import java.util.HashMap;

public final class SWEKSupplier {

    private final String supplierName;
    private final String name;
    private final String db;
    private final String key;

    private final SWEK.Source source;
    private final boolean isCactus;
    private boolean active;

    private SWEKGroup group;

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    public SWEKSupplier(String _supplierName, String _name, SWEK.Source _source, String _db) {
        supplierName = _supplierName;
        name = _name.intern();
        source = _source;
        db = _db;

        isCactus = name == "CACTus" && "HEK".equals(source.name());

        key = supplierName + source.name() + db;
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

    public String getName() {
        return name;
    }

    public void setGroup(SWEKGroup _group) {
        group = _group;
    }

    public SWEKGroup getGroup() {
        return group;
    }

    public SWEK.Source getSource() {
        return source;
    }

    public String getKey() {
        return key;
    }

    public boolean isCactus() {
        return isCactus;
    }

    public void activate(boolean b) {
        active = b;
        SWEKDownloader.activateSupplier(this, b);
    }

    public boolean isActive() {
        return active;
    }

}
