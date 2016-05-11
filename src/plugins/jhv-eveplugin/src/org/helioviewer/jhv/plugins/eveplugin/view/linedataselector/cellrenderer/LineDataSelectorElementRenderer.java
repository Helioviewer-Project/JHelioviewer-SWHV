package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataSelectorElementRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement ldse = (LineDataSelectorElement) value;

            String layerName = ldse.getName();
            if (ldse.hasData()) {
                label.setText(layerName);
                label.setToolTipText(layerName);
            } else {
                label.setText("<html><font color='gray'>" + layerName);
                label.setToolTipText(layerName + ": No data for selected interval");
            }
            label.setBorder(LineDataSelectorTablePanel.commonBorder);
        }

        return label;
    }

}
