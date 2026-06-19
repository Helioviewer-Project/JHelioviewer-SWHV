package org.helioviewer.jhv.event;

public final class SWEKSupplier {

    private final SWEKGroup group;
    private final String supplierName;
    private final String displayName;
    private final SWEK.Source source;
    private final String dbName;
    private final boolean isCactus;

    public SWEKSupplier(SWEKGroup _group, String _supplierName, String _displayName, SWEK.Source _source, String _dbName) {
        group = _group;
        supplierName = _supplierName;
        displayName = _displayName.intern();
        source = _source;
        dbName = _dbName;
        isCactus = displayName == "CACTus" && "HEK".equals(source.name());
    }

    public SWEKGroup group() {
        return group;
    }

    public String supplierName() {
        return supplierName;
    }

    public String displayName() {
        return displayName;
    }

    public SWEK.Source source() {
        return source;
    }

    public String dbName() {
        return dbName;
    }

    public boolean isCactus() {
        return isCactus;
    }
}
