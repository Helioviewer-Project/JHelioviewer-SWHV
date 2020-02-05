package org.helioviewer.jhv.events;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.Strings;
import org.helioviewer.jhv.events.gui.SWEKTreeModelElement;

public class SWEKGroup extends SWEKTreeModelElement {

    private static List<SWEKRelatedEvents> swekrelEvents;
    private final List<SWEKParameter> parameterList;

    private final ImageIcon icon;
    private final boolean containsParameterFilter;

    private List<SWEKSupplier> suppliers;
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEKParameter> _parameterList, @Nonnull ImageIcon _icon) {
        name = Strings.intern(_name);
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
                fields.put(Strings.intern(p.getParameterName()), pf.getDbType());
            }
        }
        for (SWEKRelatedEvents re : swekrelEvents) {
            if (re.getGroup() == this) {
                re.getRelatedOnList().forEach(swon -> fields.put(Strings.intern(swon.parameterFrom.getParameterName()), swon.dbType));
            }
            if (re.getRelatedWith() == this) {
                re.getRelatedOnList().forEach(swon -> fields.put(Strings.intern(swon.parameterWith.getParameterName()), swon.dbType));
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

    @Nullable
    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : parameterList) {
            if (parameter.getParameterName().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

    boolean containsFilter() {
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

    @Nonnull
    @Override
    public ImageIcon getIcon() {
        return icon;
    }

}
