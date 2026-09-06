package org.helioviewer.jhv.event.info;

import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.event.EventParameter;

@SuppressWarnings("serial")
class ParameterTableModel extends AbstractTableModel {

    private final EventParameter[] parameters;
    private final String[] urls;

    private static final int STRING_CUTOFF = 256;

    ParameterTableModel(EventParameter[] _parameters) {
        parameters = _parameters;
        urls = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Matcher matcher = Regex.HREF.matcher(parameters[i].getDisplayParameterValue());
            if (matcher.find())
                urls[i] = matcher.group(1);
        }
    }

    @Nullable
    String getURL(int rowIndex) {
        return urls[rowIndex];
    }

    @Override
    public int getRowCount() {
        return parameters.length;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 1 ? String.class : super.getColumnClass(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < parameters.length) {
            if (columnIndex == 0) {
                return parameters[rowIndex].getParameterDisplayName();
            } else if (columnIndex == 1) {
                String result = parameters[rowIndex].getDisplayParameterValue();
                return result.length() > STRING_CUTOFF && urls[rowIndex] == null ? result.substring(0, STRING_CUTOFF) + "..." : result;
            }
        }
        return "";
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Parameter Name";
        }
        if (column == 1) {
            return "Value";
        }
        return super.getColumnName(column);
    }

}
