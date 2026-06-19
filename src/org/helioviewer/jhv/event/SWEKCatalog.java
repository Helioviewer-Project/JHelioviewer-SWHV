package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SWEKCatalog {

    private static final HashMap<String, SWEK.Source> sources = new HashMap<>();
    private static final HashMap<String, SWEKGroup> groups = new HashMap<>();
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
        sources.clear();
        groups.clear();
        suppliers.clear();
        suppliersByGroup.clear();
        relatedEvents = List.of();
    }

    public static void addSource(SWEK.Source source) {
        sources.put(source.name(), source);
    }

    public static SWEK.Source getSource(String name) {
        return sources.get(name);
    }

    public static void addGroup(SWEKGroup group) {
        groups.put(group.getName(), group);
    }

    public static SWEKGroup getGroup(String name) {
        return groups.get(name);
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

    public static String key(SWEKSupplier supplier) {
        return supplier.supplierName() + supplier.source().name() + supplier.dbName();
    }

}
