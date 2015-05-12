package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class RenderableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Renderable renderable = (Renderable) value;
        String layerName = renderable.getName();
        String tooltip = "Layer name: " + layerName;

        if (renderable.isActiveImageLayer()) {
            layerName = "\u2299" + layerName;
            tooltip += " (active)";
        }
        label.setText(layerName);
        label.setBorder(RenderableContainerPanel.commonBorder);

        label.setToolTipText(tooltip);
        return label;
    }
}
