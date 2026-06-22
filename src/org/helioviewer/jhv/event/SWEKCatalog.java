package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();
    private static final HashMap<SWEKGroup, List<SWEKSupplier>> suppliersByGroup = new HashMap<>();
    private static List<SWEK.RelatedEvents> relatedEvents = List.of();

    private SWEKCatalog() {
    }

    public static void add(SWEKSupplier supplier) {
        suppliers.put(key(supplier), supplier);
        suppliersByGroup.computeIfAbsent(supplier.group(), _ -> new ArrayList<>()).add(supplier);
    }

    public static void clear() {
        suppliers.clear();
        suppliersByGroup.clear();
        relatedEvents = List.of();
    }

    public static SWEKSupplier getSupplier(String key) {
        return suppliers.get(key);
    }

    public static List<SWEKSupplier> getSuppliers(SWEKGroup group) {
        return suppliersByGroup.getOrDefault(group, List.of());
    }

    public static void setRelatedEvents(List<SWEK.RelatedEvents> events) {
        relatedEvents = events;
    }

    public static List<SWEK.RelatedEvents> getRelatedEvents() {
        return relatedEvents;
    }

    public static Map<String, String> relationDatabaseFields(SWEKGroup group) {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEK.RelatedEvents re : relatedEvents) {
            if (re.group() == group) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterFrom().intern(), swon.dbType()));
            }
            if (re.relatedWith() == group) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterWith().intern(), swon.dbType()));
            }
        }
        return fields;
    }

    public static Map<String, String> databaseFields(SWEKSupplier supplier) {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEK.Parameter p : supplier.getParameterList()) {
            SWEK.ParameterFilter pf = p.filter();
            if (pf != null)
                fields.put(p.name().intern(), pf.dbType());
        }
        fields.putAll(relationDatabaseFields(supplier.group()));
        return fields;
    }

    public static String key(SWEKSupplier supplier) {
        return supplier.supplierName() + supplier.source().name() + supplier.dbName();
    }
}
