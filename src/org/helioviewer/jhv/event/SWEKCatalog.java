package org.helioviewer.jhv.event;

import java.util.HashMap;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

    private SWEKCatalog() {
    }

    public static void add(SWEKSupplier supplier) {
        suppliers.put(key(supplier), supplier);
    }

    public static SWEKSupplier getSupplier(String key) {
        return suppliers.get(key);
    }

    public static String key(SWEKSupplier supplier) {
        return supplier.supplierName() + supplier.source().name() + supplier.dbName();
    }

}
