package org.helioviewer.jhv.plugins.swek;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.event.EventParameter;

@SuppressWarnings("serial")
class ParameterTableModel extends AbstractTableModel {

    private final EventParameter[] parameters;

    private static final int STRING_CUTOFF = 256;

    ParameterTableModel(EventParameter[] _parameters) {
        parameters = _parameters;
    }

    @Nullable
    String getURL(int rowIndex) {
        return parameters[rowIndex].getLink();
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
                EventParameter parameter = parameters[rowIndex];
                String value = parameter.getSimpleDisplayParameterValue();
                String link = parameter.getLink();
                if (link != null)
                    return "<a href=\"" + link + "\">" + (parameter.isUrl() ? "Open URL" : value) + "</a>";
                return value.length() > STRING_CUTOFF ? value.substring(0, STRING_CUTOFF) + "..." : value;
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
