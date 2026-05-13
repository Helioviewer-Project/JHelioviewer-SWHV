package org.helioviewer.jhv.layers.selector;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;

@SuppressWarnings("serial")
final class SpaceObjectTableModel extends AbstractTableModel {

    private static final int COLUMNS = 3;

    private final SpaceObjectContainer container;

    SpaceObjectTableModel(SpaceObjectContainer _container) {
        container = _container;
        container.addRefreshListener(row -> fireTableRowsUpdated(row, row));
    }

    @Override
    public int getRowCount() {
        return container.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return container.elementAt(row);
    }

}
