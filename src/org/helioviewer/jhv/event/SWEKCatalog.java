package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();
    private static final HashMap<SWEKGroup, List<SWEKSupplier>> suppliersByGroup = new HashMap<>();
    private static final HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> databaseFieldsBySupplier = new HashMap<>();
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
        databaseFieldsBySupplier.clear();
        relatedEvents = List.of();
    }

    public static SWEKSupplier getSupplier(String key) {
        return suppliers.get(key);
    }

    public static List<SWEKSupplier> getSuppliers(SWEKGroup group) {
        return suppliersByGroup.getOrDefault(group, List.of());
    }

    public static void setRelatedEvents(List<SWEK.RelatedEvents> events) {
        HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> fields = new HashMap<>();
        for (SWEKSupplier supplier : suppliers.values())
            fields.put(supplier, createDatabaseFields(supplier, events));
        relatedEvents = List.copyOf(events);
        databaseFieldsBySupplier.clear();
        databaseFieldsBySupplier.putAll(fields);
    }

    public static List<SWEK.RelatedEvents> getRelatedEvents() {
        return relatedEvents;
    }

    public static Map<String, SWEK.NumericType> databaseFields(SWEKSupplier supplier) {
        return databaseFieldsBySupplier.getOrDefault(supplier, Map.of());
    }

    private static Map<String, SWEK.NumericType> createDatabaseFields(SWEKSupplier supplier, List<SWEK.RelatedEvents> events) {
        Map<String, SWEK.NumericType> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SWEK.Parameter parameter : supplier.getParameterList()) {
            SWEK.ParameterFilter filter = parameter.filter();
            if (filter != null)
                addDatabaseField(fields, supplier, parameter.name(), filter.dbType());
        }
        for (SWEK.RelatedEvents relation : events) {
            for (SWEK.RelatedOn field : relation.relatedOnList()) {
                if (relation.group() == supplier.group())
                    addDatabaseField(fields, supplier, field.parameterFrom(), field.dbType());
                if (relation.relatedWith() == supplier.group())
                    addDatabaseField(fields, supplier, field.parameterWith(), field.dbType());
            }
        }
        return Map.copyOf(fields);
    }

    private static void addDatabaseField(Map<String, SWEK.NumericType> fields, SWEKSupplier supplier, String name, String dbType) {
        SWEK.NumericType type = SWEK.NumericType.valueOf(dbType);
        SWEK.NumericType previous = fields.putIfAbsent(name, type);
        if (previous != null && previous != type)
            throw new IllegalArgumentException("Conflicting types for " + name + " in " + key(supplier) + ": " + previous + " and " + type);
    }

    public static String key(SWEKSupplier supplier) {
        return supplier.supplierName() + supplier.source().name() + supplier.dbName();
    }
}
