package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SWEKGroup {

    private static List<SWEK.RelatedEvents> relatedEvents;

    private final String name;
    private final List<SWEK.Parameter> parameterList;
    private final String iconKey;

    private final boolean containsParameterFilter;
    private boolean downloading;
    private Runnable onDownloadingChanged;

    private final List<SWEKSupplier> suppliers = new ArrayList<>();
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEK.Parameter> _parameterList, String _iconKey) {
        name = _name.intern();
        parameterList = _parameterList;
        iconKey = _iconKey;
        containsParameterFilter = checkFilters(parameterList);
    }

    public void addSupplier(SWEKSupplier supplier) {
        suppliers.add(supplier);
        supplier.setGroup(this);
    }

    public List<SWEKSupplier> getSuppliers() {
        return suppliers;
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
        for (SWEK.RelatedEvents re : relatedEvents) {
            if (re.group() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterFrom().name().intern(), swon.dbType()));
            }
            if (re.relatedWith() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterWith().name().intern(), swon.dbType()));
            }
        }
        databaseFields = fields;
    }

    public static void setSWEKRelatedEvents(List<SWEK.RelatedEvents> _relatedEvents) {
        relatedEvents = _relatedEvents;
    }

    public static List<SWEK.RelatedEvents> getSWEKRelatedEvents() {
        return relatedEvents;
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

    public void setOnDownloadingChanged(Runnable callback) {
        onDownloadingChanged = callback;
    }

    private void setDownloading(boolean _downloading) {
        if (downloading == _downloading)
            return;
        downloading = _downloading;
        if (onDownloadingChanged != null) {
            onDownloadingChanged.run();
        }
    }

    public boolean isDownloading() {
        return downloading;
    }

    void startedDownload() {
        EventQueue.invokeLater(() -> setDownloading(true));
    }

    void stoppedDownload() {
        EventQueue.invokeLater(() -> setDownloading(false));
    }

}
