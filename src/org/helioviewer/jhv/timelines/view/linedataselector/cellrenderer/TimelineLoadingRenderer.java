package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineLoadingRenderer extends JHVTableCellRenderer {

    private final JLayer<JComponent> layer = new JLayer<>(null, UITimer.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(cellBorder);
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineRenderable && ((TimelineRenderable) value).isDownloading()) {
            table.repaint();

            layer.setForeground(label.getForeground());
            layer.setView(label);
            return layer;
        }
        return label;
    }

}
