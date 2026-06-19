package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();
    private static final HashMap<SWEKGroup, List<SWEKSupplier>> suppliersByGroup = new HashMap<>();

    private SWEKCatalog() {
    }

    public static void add(SWEKSupplier supplier) {
        suppliers.put(key(supplier), supplier);
        suppliersByGroup.computeIfAbsent(supplier.group(), _ -> new ArrayList<>()).add(supplier);
    }

    public static SWEKSupplier getSupplier(String key) {
        return suppliers.get(key);
    }

    public static List<SWEKSupplier> getSuppliers(SWEKGroup group) {
        return suppliersByGroup.getOrDefault(group, List.of());
    }

    public static String key(SWEKSupplier supplier) {
        return supplier.supplierName() + supplier.source().name() + supplier.dbName();
    }

}
