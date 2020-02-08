package org.helioviewer.jhv.events;

import java.awt.Component;
import java.awt.Image;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.interfaces.JHVTreeNode;

public class SWEKGroup implements JHVTreeNode {

    private static List<SWEKRelatedEvents> swekrelEvents;

    private final String name;
    private final List<SWEKParameter> parameterList;

    private final ImageIcon icon;
    private final JLabel label;
    private final boolean containsParameterFilter;

    private List<SWEKSupplier> suppliers;
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, List<SWEKParameter> _parameterList, ImageIcon _icon) {
        name = _name.intern();
        parameterList = _parameterList;
        icon = _icon;
        containsParameterFilter = checkFilters(parameterList);

        label = new JLabel(name);
        label.setOpaque(false);
        ComponentUtils.smallVariant(label);
        int size = label.getPreferredSize().height;
        label.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));
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
                re.getRelatedOnList().forEach(swon -> fields.put(swon.parameterFrom.getParameterName().intern(), swon.dbType));
            }
            if (re.getRelatedWith() == this) {
                re.getRelatedOnList().forEach(swon -> fields.put(swon.parameterWith.getParameterName().intern(), swon.dbType));
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

    public String getName() {
        return name;
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

    public ImageIcon getIcon() {
        return icon;
    }

    @Override
    public Component getComponent() {
        return label;
    }

}
