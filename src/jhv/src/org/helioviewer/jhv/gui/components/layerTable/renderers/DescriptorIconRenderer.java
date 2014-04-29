package org.helioviewer.jhv.gui.components.layerTable.renderers;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayerDescriptor;

/**
 * TableCellRenderer rendering the layer icon of a LayerDescriptor
 * 
 * The decision, which icon to use is based on nearly all fields of the
 * layerDescriptor
 * 
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class DescriptorIconRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = -6307424235694158409L;

    /**
     * Used border around the descriptor icon
     */
    private final Border border;

    /**
     * Creates a new renderer which shows a descriptor icon according to the
     * state
     * 
     * @param border
     */
    public DescriptorIconRenderer(Border border) {
        this.border = border;
    }

    /**
     * Sets the width for this table column to the preferred width
     * 
     * @param table
     *            Table to set
     * @param column
     *            Column this is applied to
     */
    public void setFixedWidth(JTable table, int column) {
        // Get a dummy label and measure the preferred width
        Component dummy = getTableCellRendererComponent(table, new LayerDescriptor(null, null), false, false, 0, column);
        int width = dummy.getPreferredSize().width;

        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
    }

    /**
     * Since the DefaultTableCellRenderer offers some performance optimizations
     * done to display it {@link DefaultTableCellRenderer}, we should using the
     * renderer and adopt as necessary
     * 
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LayerDescriptor) {
            // Everything is fine

            // Setup label
            LayerDescriptor descriptor = (LayerDescriptor) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
            label.setIcon(IconBank.getIcon(DescriptorIconRenderer.getIcon(descriptor)));
            label.setToolTipText(DescriptorIconRenderer.getTooltipText(descriptor));
            label.setBorder(border);

            return label;
        } else {
            return super.getTableCellRendererComponent(table, "Error", isSelected, hasFocus, row, column);
        }
    }

    private static String getTooltipText(LayerDescriptor descriptor) {
        boolean isMovie = descriptor.isMovie;
        boolean isMaster = descriptor.isMaster;
        boolean isTimed = descriptor.isTimed;
        boolean isVisible = descriptor.isVisible;

        String tooltip = "";

        if (isMovie && isTimed && isVisible && isMaster)
            tooltip = "Master Layer (Movie)";
        if (isMovie && isTimed && isVisible && !isMaster)
            tooltip = "Slave Layer (Movie)";
        if (isMovie && isTimed && !isVisible)
            tooltip = "Invisible Layer (Movie)";
        if (isMovie && !isTimed && isVisible)
            tooltip = "Layer (Movie, No timing Information)";
        if (isMovie && !isTimed && !isVisible)
            tooltip = "Invisible Layer (Movie, No timing Information)";

        if (!isMovie && isTimed && isVisible && isMaster)
            tooltip = "Master Layer (Image)";
        if (!isMovie && isTimed && isVisible && !isMaster)
            tooltip = "Slave Layer (Image)";
        if (!isMovie && isTimed && !isVisible)
            tooltip = "Invisible Layer (Image)";
        if (!isMovie && !isTimed && isVisible)
            tooltip = "Layer (Image, No timing Information)";
        if (!isMovie && !isTimed && !isVisible)
            tooltip = "Invisible Layer (Image, No timing Information)";

        if (isVisible) {
            tooltip = tooltip + " - Click to Hide";
        } else {
            tooltip = tooltip + " - Click to Unhide";
        }

        return tooltip;
    }

    /**
     * Choose the right icon for the given LayerDescriptor
     * 
     * @param descriptor
     *            - LayerDescriptor to base the selection on
     * @return the JHVIcon to draw
     */
    public static JHVIcon getIcon(LayerDescriptor descriptor) {
        boolean isMovie = descriptor.isMovie;
        boolean isMaster = descriptor.isMaster;
        boolean isTimed = descriptor.isTimed;
        boolean isVisible = descriptor.isVisible;

        JHVIcon icon = null;

        if (isMovie && isTimed && isVisible && isMaster)
            icon = JHVIcon.LAYER_MOVIE_TIME_MASTER;
        if (isMovie && isTimed && isVisible && !isMaster)
            icon = JHVIcon.LAYER_MOVIE_TIME;
        if (isMovie && isTimed && !isVisible)
            icon = JHVIcon.LAYER_MOVIE_TIME_OFF;
        if (isMovie && !isTimed && isVisible)
            icon = JHVIcon.LAYER_MOVIE;
        if (isMovie && !isTimed && !isVisible)
            icon = JHVIcon.LAYER_MOVIE_OFF;

        if (!isMovie && isTimed && isVisible && isMaster)
            icon = JHVIcon.LAYER_IMAGE_TIME_MASTER;
        if (!isMovie && isTimed && isVisible && !isMaster)
            icon = JHVIcon.LAYER_IMAGE_TIME;
        if (!isMovie && isTimed && !isVisible)
            icon = JHVIcon.LAYER_IMAGE_TIME_OFF;
        if (!isMovie && !isTimed && isVisible)
            icon = JHVIcon.LAYER_IMAGE;
        if (!isMovie && !isTimed && !isVisible)
            icon = JHVIcon.LAYER_IMAGE_OFF;

        return icon;
    }
}