package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SWEKGroup {

    private final String name;
    private final List<SWEK.Parameter> parameterList;
    private final String iconKey;

    private final boolean containsParameterFilter;

    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEK.Parameter> _parameterList, String _iconKey) {
        name = _name.intern();
        parameterList = _parameterList;
        iconKey = _iconKey;
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
        for (SWEK.RelatedEvents re : SWEKCatalog.getRelatedEvents()) {
            if (re.group() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterFrom().name().intern(), swon.dbType()));
            }
            if (re.relatedWith() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterWith().name().intern(), swon.dbType()));
            }
        }
        databaseFields = fields;
    }

    public String getName() {
        return name;
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

    public String getIconKey() {
        return iconKey;
    }

}
