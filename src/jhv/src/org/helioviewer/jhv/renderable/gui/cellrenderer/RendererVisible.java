package org.helioviewer.jhv.renderable.gui.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RendererVisible extends TableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public RendererVisible() {
        setHorizontalAlignment(CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "small");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(TableCellRenderer.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            checkBox.setSelected(((Renderable) value).isVisible());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
