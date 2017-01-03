package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.components.MoviePanel;

@SuppressWarnings("serial")
class RenderableDownloadingCellRenderer extends RenderableTableCellRenderer {

    private final JLayer<JComponent> layer = new JLayer<>(null, MoviePanel.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setText(null);
        label.setBorder(RenderableContainerPanel.commonBorder);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable && ((Renderable) value).isDownloading()) {
            table.repaint(); // lazy

            layer.setBackground(label.getBackground());
            layer.setView(label);
            return layer;
        }
        return label;
    }

}
