package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class RenderableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String layerName = ((Renderable) value).getName();
        label.setText(layerName);
        label.setBorder(RenderableContainerPanel.commonBorder);
        label.setToolTipText("Layer name: " + layerName);
        return label;
    }
}
