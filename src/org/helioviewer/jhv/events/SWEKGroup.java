package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

@SuppressWarnings("serial")
public final class SWEKGroup extends DefaultMutableTreeNode {

    private static List<SWEK.RelatedEvents> relatedEvents;

    private final String name;
    private final List<SWEK.Parameter> parameterList;
    private final ImageIcon icon;
    private final DefaultTreeModel treeModel;

    private final boolean containsParameterFilter;
    private boolean downloading;

    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEK.Parameter> _parameterList, ImageIcon _icon, DefaultTreeModel _treeModel) {
        name = _name.intern();
        parameterList = _parameterList;
        icon = _icon;
        treeModel = _treeModel;
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

    public ImageIcon getIcon() {
        return icon;
    }

    private void setDownloading(boolean _downloading) {
        if (downloading == _downloading)
            return;
        downloading = _downloading;
        treeModel.nodeChanged(this); // notify to repaint
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
