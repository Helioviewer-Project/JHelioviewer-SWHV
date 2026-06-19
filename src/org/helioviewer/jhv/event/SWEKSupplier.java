package org.helioviewer.jhv.event;

public record SWEKSupplier(SWEKGroup group, String supplierName, String name, SWEK.Source source, String dbName) {

    public SWEKSupplier {
        name = name.intern();
    }

    public boolean isCactus() {
        return name == "CACTus" && "HEK".equals(source.name());
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
