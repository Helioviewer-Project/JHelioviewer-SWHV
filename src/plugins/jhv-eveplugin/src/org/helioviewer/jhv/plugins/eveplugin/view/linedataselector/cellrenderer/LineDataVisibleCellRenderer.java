package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataVisibleCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value != null) { // In some case this can be called with value null
            LineDataSelectorElement lineDataElement = (LineDataSelectorElement) value;

            label.setText(null);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(LineDataSelectorTablePanel.commonBorder);

            if (lineDataElement.isVisible()) {
                label.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
                label.setToolTipText("Click to hide");
            } else {
                label.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
                label.setToolTipText("Click to show");
            }
        }

        return label;
    }

}
