package org.helioviewer.jhv.events.info;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.events.JHVEventParameter;

@SuppressWarnings("serial")
class ParameterTableModel extends AbstractTableModel {

    private final JHVEventParameter[] parameters;

    private static final int STRING_CUTOFF = 256;

    ParameterTableModel(JHVEventParameter[] _parameters) {
        parameters = _parameters;
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
                return result.length() > STRING_CUTOFF ? result.substring(0, STRING_CUTOFF) + "..." : result;
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
