package org.helioviewer.jhv.plugin.renderable;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class RenderableVisibleCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Renderable renderable = (Renderable) value;
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (renderable.isVisible()) {
            label.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
            label.setToolTipText("Click to hide");
        } else {
            label.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
            label.setToolTipText("Click to show");
        }
        label.setBorder(RenderableContainerPanel.commonLeftBorder);
        label.setText("");
        return label;

    }
}
