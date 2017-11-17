package org.helioviewer.jhv.data.event;

import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

public class SWEKGroup extends SWEKTreeModelElement {

    private static List<SWEKRelatedEvents> swekrelEvents;
    private final List<SWEKParameter> parameterList;

    private final boolean containsParameterFilter;

    private List<SWEKSupplier> suppliers;
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEKParameter> _parameterList, ImageIcon _icon) {
        name = _name.intern();
        parameterList = _parameterList;
        icon = _icon;
        containsParameterFilter = checkFilters(parameterList);
    }

    public HashMap<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEKParameter p : parameterList) {
            SWEKParameterFilter pf = p.getParameterFilter();
            if (pf != null) {
                fields.put(p.getParameterName().intern(), pf.getDbType());
            }
        }
        for (SWEKRelatedEvents re : swekrelEvents) {
            if (re.getGroup() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.parameterFrom;
                    fields.put(p.getParameterName().intern(), swon.dbType);
                }
            }
            if (re.getRelatedWith() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.parameterWith;
                    fields.put(p.getParameterName().intern(), swon.dbType);
                }
            }
        }
        databaseFields = fields;
    }

    public static void setSwekRelatedEvents(List<SWEKRelatedEvents> _relatedEvents) {
        swekrelEvents = _relatedEvents;
    }

    public static List<SWEKRelatedEvents> getSWEKRelatedEvents() {
        return swekrelEvents;
    }

    public List<SWEKSupplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<SWEKSupplier> _suppliers) {
        suppliers = _suppliers;
    }

    public List<SWEKParameter> getParameterList() {
        return parameterList;
    }

    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : parameterList) {
            if (parameter.getParameterName().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

    public boolean containsFilter() {
        return containsParameterFilter;
    }

    private static boolean checkFilters(List<SWEKParameter> parameters) {
        for (SWEKParameter parameter : parameters) {
            if (parameter.getParameterFilter() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void activate(boolean activate) {
        setSelected(activate);
        for (SWEKSupplier supplier : suppliers) {
            supplier.setSelected(activate);
            SWEKDownloadManager.activateSupplier(supplier, activate);
        }
    }

}
