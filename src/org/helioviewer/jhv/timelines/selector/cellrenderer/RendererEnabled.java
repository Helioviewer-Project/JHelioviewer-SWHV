package org.helioviewer.jhv.timelines.selector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public class RendererEnabled extends DefaultTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineLayer layer) {
            checkBox.setSelected(layer.isEnabled());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
