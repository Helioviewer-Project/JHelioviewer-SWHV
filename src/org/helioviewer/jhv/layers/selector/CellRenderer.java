package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.component.BusyIndicator;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
class CellRenderer {

    static final class Enabled extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // https://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof Layer layer) {
                checkBox.setSelected(layer.isEnabled());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    private static final class StatusIcon extends BusyIndicator implements Icon {

        private final Font font = Buttons.getMaterialFont(UIGlobals.uiFont.getSize2D());
        private final int size = getFontMetrics(UIGlobals.uiFont).getHeight();
        private boolean downloading;

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics g = graphics.create();
            g.translate(x, y);
            g.setColor(component.getForeground());
            if (downloading) {
                setSize(size, size);
                setForeground(component.getForeground());
                paintComponent(g);
            } else {
                g.setFont(font);
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(Buttons.check, (size - metrics.stringWidth(Buttons.check)) / 2,
                        (size - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            g.dispose();
        }

    }

    static final class Name extends DefaultTableCellRenderer {

        private final Font masterFont = UIGlobals.uiFont.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
        private final StatusIcon status = new StatusIcon();

        Name() {
            setHorizontalTextPosition(SwingConstants.LEADING);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (status.downloading) {
                Rectangle rect = table.getCellRect(row, column, false);
                table.repaint(rect.x, rect.y, rect.width, rect.height); // lazy
            }
            return component;
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Layer layer) {
                String layerName = layer.getName();
                setText(layerName);
                status.downloading = layer.isDownloading();
                setIcon(status.downloading || layer.isLocal() ? status : null);
                if (layer == Layers.getActiveImageLayer()) {
                    setToolTipText(layerName + " (master)");
                    setFont(masterFont);
                } else {
                    setToolTipText(null);
                    setFont(UIGlobals.uiFont);
                }
            }
        }

    }

    static final class Remove extends DefaultTableCellRenderer {

        private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

        @Override
        public void setValue(Object value) {
            setBorder(null); //!
            if (value instanceof Layer layer && layer.isDeletable()) {
                setFont(font);
                setText(Buttons.close);
            } else
                setText(null);
        }

    }

    static final class Time extends DefaultTableCellRenderer {

        static final Font font = UIGlobals.sansFont;

        @Override
        public void setValue(Object value) {
            if (value instanceof Layer layer) {
                setFont(font);
                setText(layer.getTimeString());
            }
        }

    }

}
