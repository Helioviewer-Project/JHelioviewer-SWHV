package org.helioviewer.jhv.plugins.hekplugin.cache;

import javax.swing.table.AbstractTableModel;

/**
 * 
 * TableModel displaying all available fields of an event. Table Layout is:
 * "Field Name", "Field Value"
 * 
 * @author mnuhn
 * 
 */
public class HEKEventTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /**
     * Event to be displayed in the table
     */
    private HEKEvent event;

    /**
     * Create a new TableModel displaying the fields of the given event
     * 
     * @param event
     *            - the event to be displayed in the table
     */
    public HEKEventTableModel(HEKEvent event) {
        this.event = event;
    }

    /**
     * Always returns "2", since we only have two columns
     */
    public int getColumnCount() {
        return 2;
    }

    /**
     * Return the number of available Fields, or zero, if no valid event is
     * stored
     */
    public int getRowCount() {
        if (event == null) {
            return 0;
        } else {
            return event.getFields().length;
        }
    }

    /**
     * Returns either the String "Field Name" or the String "Field Value"
     */
    public String getColumnName(int col) {
        if (col == 0) {
            return "Field Name";
        } else {
            return "Field Value";
        }
    }

    /**
     * Return the field name or field content for a given row,col combination
     */
    public Object getValueAt(int row, int col) {
        if (event != null && col < 2 && row < event.getFields().length) {
            if (col == 0) {
                return event.getFields()[row];
            } else {
                return event.getString(event.getFields()[row]);
            }
        } else {
            return "N/A";
        }
    }

    /**
     * Always returns String.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class getColumnClass(int c) {
        return String.class;
    }

    /**
     * All cells are NOT editable
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    /**
     * Cell values can NOT be changed
     */
    public void setValueAt(Object value, int row, int col) {
        return;
    }

}
