package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class RenderableRemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        label.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        label.setBorder(RenderableContainerPanel.commonBorder);
        label.setText("");
        label.setToolTipText("Click to remove");

        return label;

    }
}
