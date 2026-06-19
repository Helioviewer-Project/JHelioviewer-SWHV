package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SWEKSupplier {

    private final SWEKGroup group;
    private final String supplierName;
    private final String displayName;
    private final SWEK.Source source;
    private final String dbName;
    private final boolean isCactus;
    private final List<SWEK.Parameter> parameterList;
    private final boolean containsParameterFilter;

    private HashMap<String, String> databaseFields;

    public SWEKSupplier(SWEKGroup _group, String _supplierName, String _displayName, SWEK.Source _source, String _dbName, List<SWEK.Parameter> _parameterList) {
        group = _group;
        supplierName = _supplierName;
        displayName = _displayName.intern();
        source = _source;
        dbName = _dbName;
        isCactus = displayName == "CACTus" && "HEK".equals(source.name());
        parameterList = _parameterList;
        containsParameterFilter = checkFilters(parameterList);
    }

    public Map<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEK.Parameter p : parameterList) {
            SWEK.ParameterFilter pf = p.filter();
            if (pf != null) {
                fields.put(p.name().intern(), pf.dbType());
            }
        }
        fields.putAll(group.getAllDatabaseFields());
        databaseFields = fields;
    }

    public List<SWEK.Parameter> getParameterList() {
        return parameterList;
    }

    public boolean containsFilter() {
        return containsParameterFilter;
    }

    private static boolean checkFilters(List<SWEK.Parameter> parameters) {
        for (SWEK.Parameter p : parameters) {
            if (p.filter() != null) {
                return true;
            }
        }
        return false;
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
