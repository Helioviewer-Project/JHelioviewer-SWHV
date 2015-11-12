package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class RenderableTimeCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            label.setBorder(RenderableContainerPanel.commonBorder);

            String timeString = renderable.getTimeString();
            label.setText(timeString);

            if (timeString == null)
                label.setToolTipText(null);
            else
                label.setToolTipText("UTC time");
        }

        return label;
    }

}
