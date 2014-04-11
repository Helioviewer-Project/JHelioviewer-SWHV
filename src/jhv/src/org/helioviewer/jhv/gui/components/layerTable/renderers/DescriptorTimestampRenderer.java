package org.helioviewer.jhv.gui.components.layerTable.renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.layers.LayerDescriptor;

/**
 * TableCellRenderer rendering the timestamp of a LayerDescriptor
 * 
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class DescriptorTimestampRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -6307424235694158409L;
    /**
     * Flag to draw lines between entries
     */
    private final boolean drawLine;
    /**
     * Used border to seperate
     */
    private final Border interBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.gray);

    /**
     * Renderer to show the descriptor
     * 
     * @param drawLine
     *            if true it will add a gray line between layers
     */
    public DescriptorTimestampRenderer(boolean drawLine) {
        this.drawLine = drawLine;
    }

    /**
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LayerDescriptor) {
            LayerDescriptor descriptor = (LayerDescriptor) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, descriptor.timestamp, isSelected, hasFocus, row, column);
            label.setToolTipText("Shown observation time (UTC) of this layer.");
            if (drawLine && row > 0) {
                label.setBorder(interBorder);
            }
            return label;
        } else {
            return super.getTableCellRendererComponent(table, "Error", isSelected, hasFocus, row, column);
        }
    }
}