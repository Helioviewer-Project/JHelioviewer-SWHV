package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
class RenderableVisibleCellRenderer extends RenderableTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public RenderableVisibleCellRenderer() {
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "mini");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(RenderableContainerPanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            checkBox.setBackground(label.getBackground());
            checkBox.setSelected(((Renderable) value).isVisible());
            return checkBox;
        }

        label.setBorder(RenderableContainerPanel.commonBorder);
        return label;
    }

}
