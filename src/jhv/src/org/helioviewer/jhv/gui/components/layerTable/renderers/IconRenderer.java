package org.helioviewer.jhv.gui.components.layerTable.renderers;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * TableCellRenderer rendering the icon provided in the constructor
 * 
 * @author Malte Nuhn
 * @author Helge Dietert
 */
public class IconRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = -6307424235694158409L;

    /**
     * Shown border
     */
    private Border border;
    /**
     * Shown icon
     */
    private ImageIcon icon;
    /**
     * Shown tooltip
     */
    private String tooltip;

    /**
     * Creates an renderer which will show the icon if the cell is non-null
     * 
     * @param icon
     *            Icon to show
     * @param border
     *            Border around the icon
     */
    public IconRenderer(String tooltip, ImageIcon icon, Border border) {
        this.tooltip = tooltip;
        this.icon = icon;
        this.border = border;
    }

    /**
     * Changed to show the icon accordingly
     * 
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
        if (value != null) {
            label.setIcon(icon);
            label.setBorder(border);
            label.setToolTipText(tooltip);
        }

        return label;
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
        // Get a dummy label and measure the preferred width, but the table as
        // non-null object
        Component dummy = getTableCellRendererComponent(table, table, false, false, 0, column);
        int width = dummy.getPreferredSize().width;

        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
    }
}
