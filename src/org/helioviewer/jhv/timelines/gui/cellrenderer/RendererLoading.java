package org.helioviewer.jhv.timelines.gui.cellrenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public final class RendererLoading extends DefaultTableCellRenderer {

    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(null); //!
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineLayer layer && layer.isDownloading()) {
            table.repaint(); // lazy

            over.setForeground(label.getForeground());
            over.setView(label);
            return over;
        }
        return label;
    }

}
