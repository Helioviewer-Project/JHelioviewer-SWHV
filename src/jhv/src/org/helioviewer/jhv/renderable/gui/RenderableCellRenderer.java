package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;

@SuppressWarnings("serial")
public class RenderableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            String layerName = renderable.getName();
            String tooltip = layerName;

            if (renderable instanceof RenderableImageLayer && ((RenderableImageLayer) renderable).isActiveImageLayer()) {
                tooltip += " (master)";
                label.setToolTipText(tooltip);
                label.setFont(UIGlobals.UIFontBold);
            } else {
                label.setToolTipText(null);
                label.setFont(UIGlobals.UIFont);
            }

            label.setText(layerName);
            label.setBorder(RenderableContainerPanel.commonBorder);
        }

        return label;
    }

}
