package org.helioviewer.jhv.plugin.renderable;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.layers.LayerDescriptor;

public class RenderableCellRenderer extends DefaultTableCellRenderer {

    public void setFixedWidth(JTable table, int column) {
        Component dummy = getTableCellRendererComponent(table, new LayerDescriptor(null, null), false, false, 0, column);
        int width = dummy.getPreferredSize().width;

        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        label.setText(((Renderable) value).getName());
        label.setBorder(new MatteBorder(1, 0, 0, 0, Color.BLACK));
        return label;
    }
}
