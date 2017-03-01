package org.helioviewer.jhv.renderable.gui.cellrenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RendererLoading extends TableCellRenderer {

    private final JLayer<JComponent> layer = new JLayer<>(null, MoviePanel.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(TableCellRenderer.commonBorder);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable && ((Renderable) value).isDownloading()) {
            table.repaint(); // lazy

            layer.setForeground(label.getForeground());
            layer.setView(label);
            return layer;
        }
        return label;
    }

}
