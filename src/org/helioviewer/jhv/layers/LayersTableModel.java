package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.selector.Reorderable;

@SuppressWarnings("serial")
public final class LayersTableModel extends AbstractTableModel implements Layers.Listener, Reorderable {

    public static final int TIME_COL = 2;
    public static final int NUMBER_COLUMNS = 5;

    public LayersTableModel() {
        Layers.addListener(this);
    }

    @Override
    public int getRowCount() {
        return Layers.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return NUMBER_COLUMNS;
    }

    @Nullable
    @Override
    public Object getValueAt(int row, int col) {
        return Layers.getValueAt(row);
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        Layers.reorder(fromIndex, toIndex);
        fireTableDataChanged();
    }

    @Override
    public void rowsInserted(int firstRow, int lastRow) {
        fireTableRowsInserted(firstRow, lastRow);
    }

    @Override
    public void rowsDeleted(int firstRow, int lastRow) {
        fireTableRowsDeleted(firstRow, lastRow);
    }

    @Override
    public void cellUpdated(int row, int col) {
        fireTableCellUpdated(row, col);
    }

    @Override
    public void timeUpdated(Layer layer) {
        int row = Layers.getLayers().indexOf(layer);
        if (row >= 0)
            fireTableCellUpdated(row, TIME_COL);
    }

    public void updateCell(int row, int col) {
        if (row >= 0)
            fireTableCellUpdated(row, col);
    }

}
