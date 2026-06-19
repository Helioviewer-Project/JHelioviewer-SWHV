package org.helioviewer.jhv.event;

public final class SWEKSupplier {

    private final String supplierName;
    private final String name;
    private final String db;

    private final SWEK.Source source;
    private final SWEKGroup group;
    private final boolean isCactus;

    public SWEKSupplier(SWEKGroup _group, String _supplierName, String _name, SWEK.Source _source, String _db) {
        group = _group;
        supplierName = _supplierName;
        name = _name.intern();
        source = _source;
        db = _db;

        isCactus = name == "CACTus" && "HEK".equals(source.name());
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

    public SWEKGroup getGroup() {
        return group;
    }

    public SWEK.Source getSource() {
        return source;
    }

    public boolean isCactus() {
        return isCactus;
    }

}
