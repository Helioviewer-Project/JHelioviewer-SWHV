package org.helioviewer.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class LineDataVisibleCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        LineDataSelectorElement lineDataElement = (LineDataSelectorElement) value;
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (lineDataElement.isVisible()) {
            label.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
            label.setToolTipText("Click to hide");
        } else {
            label.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
            label.setToolTipText("Click to show");
        }
        label.setText("");
        label.setBorder(LineDateSelectorTablePanel.commonLeftBorder);
        return label;

    }

}
