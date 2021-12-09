package org.helioviewer.jhv.layers.selector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class RendererEnabled extends DefaultTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Layer) {
            checkBox.setSelected(((Layer) value).isEnabled());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
