package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings({ "serial" })
public class RenderableTimeCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Renderable renderable = ImageViewerGui.getRenderableContainer().getTypedValueAt(row, column);
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setText(renderable.getTimeString());
        label.setBorder(RenderableContainerPanel.commonBorder);
        label.setToolTipText("UTC observation time");

        return label;
    }

}
