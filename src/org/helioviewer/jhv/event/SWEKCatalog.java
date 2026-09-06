package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class SWEKCatalog {

    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();
    private static final HashMap<SWEKGroup, List<SWEKSupplier>> suppliersByGroup = new HashMap<>();
    private static final HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> indexedParametersBySupplier = new HashMap<>();
    private static List<SWEK.Relation> relations = List.of();

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
        relations = List.of();
    }

    public static SWEKSupplier getSupplier(String id) {
        return suppliers.get(id);
    }

    public static List<SWEKSupplier> getSuppliers(SWEKGroup group) {
        return suppliersByGroup.getOrDefault(group, List.of());
    }

    public static void setRelations(List<SWEK.Relation> definitions) {
        HashMap<SWEKSupplier, Map<String, SWEK.NumericType>> fields = new HashMap<>();
        for (SWEKSupplier supplier : suppliers.values())
            fields.put(supplier, createIndexedParameters(supplier, definitions));
        for (SWEK.Relation relation : definitions) {
            for (SWEKSupplier from : getSuppliers(relation.group())) {
                for (SWEKSupplier with : getSuppliers(relation.relatedWith())) {
                    for (SWEK.RelatedOn field : relation.relatedOnList()) {
                        if (from.source().numericParameters().get(field.parameterFrom()) != with.source().numericParameters().get(field.parameterWith()))
                            throw new IllegalArgumentException("Incompatible numeric types for relationship " + field + " between " + from.id() + " and " + with.id());
                    }
                }
            }
        }
        relations = List.copyOf(definitions);
        indexedParametersBySupplier.clear();
        indexedParametersBySupplier.putAll(fields);
    }

    public static List<SWEK.Relation> getRelations() {
        return relations;
    }

    public static Map<String, SWEK.NumericType> indexedParameters(SWEKSupplier supplier) {
        return indexedParametersBySupplier.getOrDefault(supplier, Map.of());
    }

    private static Map<String, SWEK.NumericType> createIndexedParameters(SWEKSupplier supplier, List<SWEK.Relation> definitions) {
        Map<String, SWEK.NumericType> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SWEK.Parameter parameter : supplier.getParameterList()) {
            SWEK.ParameterFilter filter = parameter.filter();
            if (filter != null)
                addIndexedParameter(fields, supplier, parameter.name());
        }
        for (SWEK.Relation relation : definitions) {
            for (SWEK.RelatedOn field : relation.relatedOnList()) {
                if (relation.group() == supplier.group())
                    addIndexedParameter(fields, supplier, field.parameterFrom());
                if (relation.relatedWith() == supplier.group())
                    addIndexedParameter(fields, supplier, field.parameterWith());
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    private static void addIndexedParameter(Map<String, SWEK.NumericType> fields, SWEKSupplier supplier, String name) {
        SWEK.NumericType type = supplier.source().numericParameters().get(name);
        if (type == null)
            throw new IllegalArgumentException("Missing numeric definition for " + name + " in " + supplier.source().name());
        fields.putIfAbsent(name, type);
    }

}
