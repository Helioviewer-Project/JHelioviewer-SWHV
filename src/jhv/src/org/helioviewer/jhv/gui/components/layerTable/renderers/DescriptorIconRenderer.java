package org.helioviewer.jhv.gui.components.layerTable.renderers;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.base.Pair;
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
            // Setup label
            LayerDescriptor descriptor = (LayerDescriptor) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);

            Pair<Icon, String> pair = DescriptorIconRenderer.getIconTooltip(descriptor);
            label.setIcon(pair.a);
            label.setToolTipText(pair.b);
            label.setBorder(border);

            return label;
        } else {
            return super.getTableCellRendererComponent(table, "Error", isSelected, hasFocus, row, column);
        }
    }

    private static final Icon iconLAYER_MOVIE_TIME_MASTER = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME_MASTER);
    private static final Icon iconLAYER_MOVIE_TIME = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME);
    private static final Icon iconLAYER_MOVIE_TIME_OFF = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME_OFF);
    private static final Icon iconLAYER_MOVIE = IconBank.getIcon(JHVIcon.LAYER_MOVIE);
    private static final Icon iconLAYER_MOVIE_OFF = IconBank.getIcon(JHVIcon.LAYER_MOVIE_OFF);

    private static final Icon iconLAYER_IMAGE_TIME_MASTER = IconBank.getIcon(JHVIcon.LAYER_IMAGE_TIME_MASTER);
    private static final Icon iconLAYER_IMAGE_TIME = IconBank.getIcon(JHVIcon.LAYER_IMAGE_TIME);
    private static final Icon iconLAYER_IMAGE_TIME_OFF = IconBank.getIcon(JHVIcon.LAYER_IMAGE_TIME_OFF);
    private static final Icon iconLAYER_IMAGE = IconBank.getIcon(JHVIcon.LAYER_IMAGE);
    private static final Icon iconLAYER_IMAGE_OFF = IconBank.getIcon(JHVIcon.LAYER_IMAGE_OFF);

    /**
     * Choose the right icon for the given LayerDescriptor
     * 
     * @param descriptor
     *            - LayerDescriptor to base the selection on
     * @return the JHVIcon to draw
     */
    public static Pair<Icon, String> getIconTooltip(LayerDescriptor descriptor) {
        boolean isMovie = descriptor.isMovie;
        boolean isMaster = descriptor.isMaster;
        boolean isTimed = descriptor.isTimed;
        boolean isVisible = descriptor.isVisible;

        Icon icon = null;
        String tooltip = "";

        if (isMovie && isTimed && isVisible && isMaster) {
            icon = iconLAYER_MOVIE_TIME_MASTER;
            tooltip = "Master layer (movie)";
        }
        if (isMovie && isTimed && isVisible && !isMaster) {
            icon = iconLAYER_MOVIE_TIME;
            tooltip = "Slave layer (movie)";
        }
        if (isMovie && isTimed && !isVisible) {
            icon = iconLAYER_MOVIE_TIME_OFF;
            tooltip = "Invisible layer (movie)";
        }
        if (isMovie && !isTimed && isVisible) {
            icon = iconLAYER_MOVIE;
            tooltip = "Layer (movie, no timing information)";
        }
        if (isMovie && !isTimed && !isVisible) {
            icon = iconLAYER_MOVIE_OFF;
            tooltip = "Invisible layer (movie, no timing information)";
        }

        if (!isMovie && isTimed && isVisible && isMaster) {
            icon = iconLAYER_IMAGE_TIME_MASTER;
            tooltip = "Master layer (image)";
        }
        if (!isMovie && isTimed && isVisible && !isMaster) {
            icon = iconLAYER_IMAGE_TIME;
            tooltip = "Slave layer (image)";
        }
        if (!isMovie && isTimed && !isVisible) {
            icon = iconLAYER_IMAGE_TIME_OFF;
            tooltip = "Invisible layer (image)";
        }
        if (!isMovie && !isTimed && isVisible) {
            icon = iconLAYER_IMAGE;
            tooltip = "Layer (image, no timing information)";
        }
        if (!isMovie && !isTimed && !isVisible) {
            icon = iconLAYER_IMAGE_OFF;
            tooltip = "Invisible layer (image, no timing information)";
        }

        if (isVisible) {
            tooltip += " - click to hide";
        } else {
            tooltip += " - click to unhide";
        }

        return new Pair<Icon, String>(icon, tooltip);
    }

}
