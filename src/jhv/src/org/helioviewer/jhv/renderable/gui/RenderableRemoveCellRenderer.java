package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings({ "serial" })
public class RenderableRemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Renderable renderable = ImageViewerGui.getRenderableContainer().getTypedValueAt(row, column);
        if (renderable.isDeletable()) {
            label.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
            label.setToolTipText("Click to remove");
        } else {
            label.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER_GRAY));
            label.setToolTipText("Cannot be removed");
        }
        label.setBorder(RenderableContainerPanel.commonRightBorder);
        label.setText(null);

        return label;

    }

}
