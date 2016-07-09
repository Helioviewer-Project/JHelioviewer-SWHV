package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataVisibleCellRenderer extends DefaultTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public LineDataVisibleCellRenderer() {
        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "mini");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(LineDataSelectorTablePanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement) {
            checkBox.setBackground(label.getBackground());
            checkBox.setSelected(((LineDataSelectorElement) value).isVisible());
            return checkBox;
        }

        label.setBorder(LineDataSelectorTablePanel.commonBorder);
        return label;
    }

}
