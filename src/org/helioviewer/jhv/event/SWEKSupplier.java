package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public final class SWEKSupplier {

    private final SWEKGroup group;
    private final String supplierName;
    private final String displayName;
    private final SWEK.Source source;
    private final String id;
    private final boolean isCactus;
    private final List<SWEK.Parameter> parameterList;
    private final HashMap<String, SWEK.Parameter> parametersByName;
    private final boolean containsParameterFilter;

    public SWEKSupplier(SWEKGroup _group, String _supplierName, String _displayName, SWEK.Source _source, String _id, List<SWEK.Parameter> _parameterList) {
        group = _group;
        supplierName = _supplierName;
        displayName = _displayName.intern();
        source = _source;
        id = _id;
        isCactus = displayName == "CACTus" && "HEK".equals(source.name());
        parameterList = _parameterList;
        parametersByName = indexParameters(parameterList, source.generalParameters());
        containsParameterFilter = checkFilters(parameterList);
    }

    public List<SWEK.Parameter> getParameterList() {
        return parameterList;
    }

    public boolean containsFilter() {
        return containsParameterFilter;
    }

    @Nullable
    SWEK.Parameter findParameter(String name) {
        return parametersByName.get(name.toLowerCase());
    }

    private static HashMap<String, SWEK.Parameter> indexParameters(List<SWEK.Parameter> parameters,
                                                                   List<SWEK.Parameter> generalParameters) {
        HashMap<String, SWEK.Parameter> byName = new HashMap<>();
        for (SWEK.Parameter parameter : parameters)
            byName.putIfAbsent(parameter.name().toLowerCase(), parameter);
        for (SWEK.Parameter parameter : generalParameters)
            byName.putIfAbsent(parameter.name().toLowerCase(), parameter);
        return byName;
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

    public String id() {
        return id;
    }

    public boolean isCactus() {
        return isCactus;
    }
}
