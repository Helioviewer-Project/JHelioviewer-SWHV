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
 * TableCellRenderer rendering the name of a LayerDescriptor
 *
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class DescriptorTitleRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = -6307424235694158409L;

    /**
     * Flag to draw lines between entries
     */
    private final boolean drawLine;
    /**
     * Used border to seperate
     */
    private final Border interBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray);

    /**
     * Renderer to show the descriptor
     *
     * @param drawLine
     *            if true it will add a gray line between layers
     */
    public DescriptorTitleRenderer(boolean drawLine) {
        this.drawLine = drawLine;
    }

    /**
     * Adopted to show the descriptor of the layer
     *
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LayerDescriptor) {
            LayerDescriptor descriptor = (LayerDescriptor) value;
            String layerName = descriptor.observatory + " " + descriptor.title;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, layerName, isSelected, hasFocus, row, column);
            label.setToolTipText("Layer name: " + layerName);
            //if (drawLine && row > 0) {
            label.setBorder(interBorder);
            //}
            return label;
        } else {
            return super.getTableCellRendererComponent(table, "Error", isSelected, hasFocus, row, column);
        }
    }
}