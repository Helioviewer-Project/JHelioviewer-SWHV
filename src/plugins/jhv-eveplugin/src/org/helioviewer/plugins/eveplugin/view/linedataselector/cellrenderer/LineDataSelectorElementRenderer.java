package org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class LineDataSelectorElementRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String layerName = ((LineDataSelectorElement) value).getName();
        label.setText(layerName);
        label.setToolTipText("Line or spectrogram name: " + layerName);
        label.setBorder(LineDateSelectorTablePanel.commonBorder);
        return label;
    }

}
