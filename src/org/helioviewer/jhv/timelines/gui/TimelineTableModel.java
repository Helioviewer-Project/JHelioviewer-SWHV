package org.helioviewer.jhv.timelines.gui;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.TimelineLayers;

@SuppressWarnings("serial")
final class TimelineTableModel extends AbstractTableModel implements TimelineLayers.Listener {

    static final int ENABLED_COLUMN = 0;
    static final int TITLE_COLUMN = 1;
    static final int LOADING_COLUMN = 2;
    static final int APPEARANCE_COLUMN = 3;
    static final int REMOVE_COLUMN = 4;
    private static final int COLUMN_COUNT = 5;

    TimelineTableModel() {
        TimelineLayers.addListener(this);
    }

    @Override
    public int getRowCount() {
        return TimelineLayers.get().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return TimelineLayers.get().get(row);
    }

    void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    @Override
    public void layersAdded(int firstRow, int lastRow) {
        fireTableRowsInserted(firstRow, lastRow);
    }

    @Override
    public void layerRemoved(int row) {
        fireTableRowsDeleted(row, row);
    }

    @Override
    public void layersReplaced() {
        fireTableDataChanged();
    }

    @Override
    public void layersUpdated(int firstRow, int lastRow) {
        fireTableRowsUpdated(firstRow, lastRow);
    }

}
