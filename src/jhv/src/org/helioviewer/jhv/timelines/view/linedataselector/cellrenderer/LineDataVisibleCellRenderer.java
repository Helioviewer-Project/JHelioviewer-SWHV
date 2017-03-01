package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.timelines.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataVisibleCellRenderer extends DefaultTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public LineDataVisibleCellRenderer() {
        setHorizontalAlignment(CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "small");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(LineDataSelectorTablePanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement) {
            checkBox.setSelected(((LineDataSelectorElement) value).isVisible());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
