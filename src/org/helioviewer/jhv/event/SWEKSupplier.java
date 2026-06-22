package org.helioviewer.jhv.event;

import java.util.List;

public final class SWEKSupplier {

    private final SWEKGroup group;
    private final String supplierName;
    private final String displayName;
    private final SWEK.Source source;
    private final String dbName;
    private final boolean isCactus;
    private final List<SWEK.Parameter> parameterList;
    private final boolean containsParameterFilter;

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
