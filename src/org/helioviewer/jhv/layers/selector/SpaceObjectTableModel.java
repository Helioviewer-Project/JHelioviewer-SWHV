package org.helioviewer.jhv.layers.selector;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectModel;

@SuppressWarnings("serial")
final class SpaceObjectTableModel extends AbstractTableModel {

    private static final int COLUMNS = 3;

    private final SpaceObjectModel model;

    SpaceObjectTableModel(SpaceObjectModel _model) {
        model = _model;
        model.addRefreshListener(this::refresh);
    }

    private void refresh(SpaceObjectElement element) {
        int row = model.indexOf(element);
        if (row != -1)
            fireTableRowsUpdated(row, row);
    }

    @Override
    public int getRowCount() {
        return model.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return model.elementAt(row);
    }

}
