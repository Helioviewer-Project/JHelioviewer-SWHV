package org.helioviewer.jhv.renderable.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
class RenderableVisibleCellRenderer extends RenderableTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();
    private final Color defaultColor = getBackground();

    public RenderableVisibleCellRenderer() {
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "mini");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(RenderableContainerPanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            checkBox.setSelected(((Renderable) value).isVisible());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : defaultColor);
        return checkBox;
    }

}
