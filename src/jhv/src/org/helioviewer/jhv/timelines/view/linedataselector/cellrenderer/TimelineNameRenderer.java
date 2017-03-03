package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineNameRenderer extends JHVTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(cellBorder);
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineRenderable) {
            TimelineRenderable ldse = (TimelineRenderable) value;
            String layerName = ldse.getName();
            if (ldse.hasData()) {
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
