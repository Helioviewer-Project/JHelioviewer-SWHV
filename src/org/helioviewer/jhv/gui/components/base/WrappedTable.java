package org.helioviewer.jhv.gui.components.base;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class WrappedTable extends JTable {

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        updateRowHeights();
    }

    // don't delete
    // @Override
    // public void tableChanged(TableModelEvent e) {
    //     updateRowHeights();
    // }

    private void updateRowHeights() {
        for (int i = 0; i < getRowCount(); i++) {
            int height = 0;
            for (int j = 0; j < getColumnCount(); j++) {
                Component comp = prepareRenderer(getCellRenderer(i, j), i, j);
                Dimension dim = comp.getPreferredSize();
                if (dim != null && dim.height > height) // satisfy coverity
                    height = dim.height;
            }
            setRowHeight(i, height);
        }
    }

    public static class WrappedTextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(String.format("<html><div width=%d>%s</div>", table.getColumnModel().getColumn(column).getWidth(), value));
            return label;
        }
    }

}
