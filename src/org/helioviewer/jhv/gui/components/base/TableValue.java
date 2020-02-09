package org.helioviewer.jhv.gui.components.base;

import java.awt.Point;

import javax.annotation.Nullable;
import javax.swing.JTable;

public class TableValue {

    public final int row;
    public final int col;
    public final Object value;

    private TableValue(int _row, int _col, Object _value) {
        row = _row;
        col = _col;
        value = _value;
    }

    @Nullable
    public static TableValue tableValueAtPoint(JTable table, Point point) {
        int row = table.rowAtPoint(point);
        int col = table.columnAtPoint(point);
        if (row == -1 || col == -1)
            return null;
        return new TableValue(row, col, table.getModel().getValueAt(row, col));
    }

}
