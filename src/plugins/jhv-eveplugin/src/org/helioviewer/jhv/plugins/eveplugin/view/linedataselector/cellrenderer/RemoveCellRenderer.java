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

//Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class RemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null) { // In some case this can be called with value null
            // (getAccessibleChild(int i) of JTable )
            LineDataSelectorElement lineDataElement = (LineDataSelectorElement) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            label.setText(null);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(LineDataSelectorTablePanel.commonBorder);

            if (lineDataElement.isDeletable()) {
                label.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
                label.setToolTipText("Click to remove");
            } else {
                label.setIcon(null);
                label.setToolTipText(null);
            }

            return label;
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}
