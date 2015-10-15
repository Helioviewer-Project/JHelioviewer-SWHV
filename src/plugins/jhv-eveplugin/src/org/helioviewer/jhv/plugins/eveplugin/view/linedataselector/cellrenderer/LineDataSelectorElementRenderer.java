package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

//Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class LineDataSelectorElementRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null) { // In some case this can be called with value null
            // (getAccessibleChild(int i) of JTable )
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            LineDataSelectorElement ldse = (LineDataSelectorElement) value;
            String layerName = ldse.getName();
            if (ldse.hasData()) {
                label.setText(layerName);
                label.setToolTipText(layerName);
            } else {
                label.setText("<html><font color='red'>" + layerName);
                label.setToolTipText(layerName + ": No data for selected interval");
            }
            label.setBorder(LineDataSelectorTablePanel.commonBorder);
            return label;
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}
