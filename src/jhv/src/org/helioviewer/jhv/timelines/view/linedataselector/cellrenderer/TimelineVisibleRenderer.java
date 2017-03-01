package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.view.linedataselector.TimelinePanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineVisibleRenderer extends DefaultTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public TimelineVisibleRenderer() {
        setHorizontalAlignment(CENTER);
        checkBox.putClientProperty("JComponent.sizeVariant", "small");
        checkBox.setBorderPainted(true);
        checkBox.setBorder(TimelinePanel.commonBorder);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof TimelineRenderable) {
            checkBox.setSelected(((TimelineRenderable) value).isVisible());
        }
        checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return checkBox;
    }

}
