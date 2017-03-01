package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LoadingCellRenderer extends DefaultTableCellRenderer {

    private final JLayer<JComponent> layer = new JLayer<>(null, MoviePanel.busyIndicator);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(LineDataSelectorTablePanel.commonBorder);
        label.setText(null);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement && ((LineDataSelectorElement) value).isDownloading()) {
            table.repaint();

            layer.setForeground(label.getForeground());
            layer.setView(label);
            return layer;
        }
        return label;
    }

}
