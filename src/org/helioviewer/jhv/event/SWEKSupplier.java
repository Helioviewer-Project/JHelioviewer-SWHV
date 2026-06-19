package org.helioviewer.jhv.event;

public record SWEKSupplier(SWEKGroup group, String supplierName, String displayName, SWEK.Source source, String dbName) {

    public SWEKSupplier {
        displayName = displayName.intern();
    }

    public boolean isCactus() {
        return displayName == "CACTus" && "HEK".equals(source.name());
    }

    @Override
    public boolean equals(Object other) {
        return other == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

}
