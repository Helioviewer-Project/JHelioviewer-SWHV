package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class RenderableRemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            if (renderable.isDeletable()) {
                label.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
                label.setToolTipText("Click to remove");
            } else {
                label.setIcon(null); // IconBank.getIcon(JHVIcon.REMOVE_LAYER_GRAY))
                label.setToolTipText(null); // "Cannot be removed"
            }
            label.setBorder(RenderableContainerPanel.commonRightBorder);
            label.setText(null);
        }

        return label;
    }

}
