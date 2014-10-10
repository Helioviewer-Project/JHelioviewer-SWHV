package org.helioviewer.jhv.data.guielements.model;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;

/**
 * The model for parameter table panel.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ParameterTableModel extends AbstractTableModel {

    /** The parameters in this model */
    private final List<JHVEventParameter> parameters;

    /** UID */
    private static final long serialVersionUID = 1963880595668739342L;

    /**
     * Creates a parameter model for the given parameters.
     * 
     * @param parameters
     *            the parameters
     */
    public ParameterTableModel(List<JHVEventParameter> parameters) {
        super();
        this.parameters = parameters;
    }

    @Override
    public int getRowCount() {
        return parameters.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < parameters.size()) {
            if (columnIndex == 0) {
                return parameters.get(rowIndex).getParameterDisplayName();
            } else if (columnIndex == 1) {
                return parameters.get(rowIndex).getParameterValue();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Parameter Name";
        } else if (column == 1) {
            return "Value";
        } else {
            return super.getColumnName(column);
        }
    }

}
