package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.components.base.BusyIndicator.BusyLabel;

@SuppressWarnings("serial")
class RenderableDownloadingCellRenderer extends RenderableTableCellRenderer {

    private final BusyLabel busy;

    public RenderableDownloadingCellRenderer() {
        busy = new BusyLabel();
        busy.setBorder(RenderableContainerPanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable && ((Renderable) value).isDownloading()) {
            busy.setBackground(label.getBackground());
            table.repaint(); // lazy
            return busy;
        }

        label.setText(null);
        label.setBorder(RenderableContainerPanel.commonBorder);
        return label;
    }

}
