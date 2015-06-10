package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings({ "serial" })
public class RenderableVisibleCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value == null)
            return label;

        Renderable renderable = (Renderable) value;
        if (renderable.isVisible()) {
            label.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
            label.setToolTipText("Click to hide");
        } else {
            label.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
            label.setToolTipText("Click to show");
        }
        label.setBorder(RenderableContainerPanel.commonLeftBorder);
        label.setText(null);
        return label;
    }

}
