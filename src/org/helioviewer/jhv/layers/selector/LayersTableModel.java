package org.helioviewer.jhv.layers.selector;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
final class LayersTableModel extends AbstractTableModel implements Layers.Listener, Reorderable {

    static final int NAME_COL = 1;
    static final int TIME_COL = 2;
    static final int NUMBER_COLUMNS = 5;

    LayersTableModel() {
        Layers.addListener(this);
    }

    @Override
    public int getRowCount() {
        return Layers.getLayers().size();
    }

    @Override
    public int getColumnCount() {
        return NUMBER_COLUMNS;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return Layers.getLayers().get(row);
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        Layers.reorderImageLayer(fromIndex, toIndex);
        fireTableDataChanged();
    }

    @Override
    public void layerAdded(int index, Layer layer) {
        fireTableRowsInserted(index, index);
    }

    @Override
    public void layerRemoved(int index, Layer layer) {
        fireTableRowsDeleted(index, index);
    }

    @Override
    public void layersCleared() {
        fireTableDataChanged();
    }

    @Override
    public void nameUpdated(Layer layer) {
        int row = Layers.getLayers().indexOf(layer);
        if (row >= 0)
            fireTableCellUpdated(row, NAME_COL);
    }

    @Override
    public void layerUpdated(Layer layer) {
        int row = Layers.getLayers().indexOf(layer);
        if (row >= 0)
            fireTableRowsUpdated(row, row);
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
