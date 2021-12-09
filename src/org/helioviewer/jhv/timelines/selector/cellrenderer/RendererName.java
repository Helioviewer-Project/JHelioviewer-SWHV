package org.helioviewer.jhv.timelines.selector.cellrenderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public class RendererName extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineLayer layer) {
            String layerName = layer.getName();
            if (layer.hasData()) {
                label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                label.setToolTipText(layerName);
            } else {
                label.setForeground(Color.GRAY);
                label.setToolTipText(layerName + ": No data for selected interval");
            }
            label.setText(layerName);
        }
        return label;
    }

}
