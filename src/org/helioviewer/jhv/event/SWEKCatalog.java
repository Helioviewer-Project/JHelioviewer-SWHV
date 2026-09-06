package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();
    private static final HashMap<SWEKGroup, List<SWEKSupplier>> suppliersByGroup = new HashMap<>();
    private static final HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> indexedParametersBySupplier = new HashMap<>();
    private static List<SWEK.RelatedEvents> relatedEvents = List.of();

    private SWEKCatalog() {
    }

    public static void add(SWEKSupplier supplier) {
        if (suppliers.putIfAbsent(supplier.id(), supplier) != null)
            throw new IllegalArgumentException("Duplicate supplier ID: " + supplier.id());
        suppliersByGroup.computeIfAbsent(supplier.group(), _ -> new ArrayList<>()).add(supplier);
    }

    public static void clear() {
        suppliers.clear();
        suppliersByGroup.clear();
        indexedParametersBySupplier.clear();
        relatedEvents = List.of();
    }

    public static SWEKSupplier getSupplier(String id) {
        return suppliers.get(id);
    }

    public static List<SWEKSupplier> getSuppliers(SWEKGroup group) {
        return suppliersByGroup.getOrDefault(group, List.of());
    }

    public static void setRelatedEvents(List<SWEK.RelatedEvents> events) {
        HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> fields = new HashMap<>();
        for (SWEKSupplier supplier : suppliers.values())
            fields.put(supplier, createIndexedParameters(supplier, events));
        for (SWEK.RelatedEvents relation : events) {
            for (SWEKSupplier from : getSuppliers(relation.group())) {
                for (SWEKSupplier with : getSuppliers(relation.relatedWith())) {
                    for (SWEK.RelatedOn field : relation.relatedOnList()) {
                        if (from.source().numericParameters().get(field.parameterFrom()) != with.source().numericParameters().get(field.parameterWith()))
                            throw new IllegalArgumentException("Incompatible numeric types for relationship " + field + " between " + from.id() + " and " + with.id());
                    }
                }
            }
        }
        relatedEvents = List.copyOf(events);
        indexedParametersBySupplier.clear();
        indexedParametersBySupplier.putAll(fields);
    }

    public static List<SWEK.RelatedEvents> getRelatedEvents() {
        return relatedEvents;
    }

    public static Map<String, SWEK.NumericType> indexedParameters(SWEKSupplier supplier) {
        return indexedParametersBySupplier.getOrDefault(supplier, Map.of());
    }

    private static Map<String, SWEK.NumericType> createIndexedParameters(SWEKSupplier supplier, List<SWEK.RelatedEvents> events) {
        Map<String, SWEK.NumericType> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SWEK.Parameter parameter : supplier.getParameterList()) {
            SWEK.ParameterFilter filter = parameter.filter();
            if (filter != null)
                addIndexedParameter(fields, supplier, parameter.name());
        }
        for (SWEK.RelatedEvents relation : events) {
            for (SWEK.RelatedOn field : relation.relatedOnList()) {
                if (relation.group() == supplier.group())
                    addIndexedParameter(fields, supplier, field.parameterFrom());
                if (relation.relatedWith() == supplier.group())
                    addIndexedParameter(fields, supplier, field.parameterWith());
            }
        }
        return Map.copyOf(fields);
    }

    private static void addIndexedParameter(Map<String, SWEK.NumericType> fields, SWEKSupplier supplier, String name) {
        SWEK.NumericType type = supplier.source().numericParameters().get(name);
        if (type == null)
            throw new IllegalArgumentException("Missing numeric definition for " + name + " in " + supplier.source().name());
        fields.putIfAbsent(name, type);
    }

}
