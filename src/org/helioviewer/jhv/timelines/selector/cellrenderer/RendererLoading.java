package org.helioviewer.jhv.timelines.selector.cellrenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public class RendererLoading extends JHVTableCellRenderer {

    private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(null); //!
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineLayer && ((TimelineLayer) value).isDownloading()) {
            table.repaint(); // lazy

            over.setForeground(label.getForeground());
            over.setView(label);
            return over;
        }
        return label;
    }

}
