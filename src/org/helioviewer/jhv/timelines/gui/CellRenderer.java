package org.helioviewer.jhv.timelines.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.component.BusyIndicator;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.band.Band;

@SuppressWarnings("serial")
class CellRenderer {

    static final class Enabled extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // https://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof TimelineLayer layer) {
                checkBox.setSelected(layer.isEnabled());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    static final class LineColor extends DefaultTableCellRenderer {

        private Color c;
        private boolean multicolor;

        @Override
        public void setValue(Object value) {
            if (value instanceof Band band) {
                c = band.getDataColor();
                multicolor = band.isMulticolor();
            } else if (value instanceof TimelineLayer layer) {
                c = layer.getDataColor();
                multicolor = false;
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (c != null) {
                int w = getWidth() - 4;
                int h = 2;
                int y = getHeight() / 2 - 1;
                if (multicolor) {
                    g.setColor(Color.RED);
                    g.fillRect(4, y, w / 2, h);
                    g.setColor(Color.GREEN);
                    g.fillRect(4 + w / 2, y, w - w / 2, h);
                } else {
                    g.setColor(c);
                    g.fillRect(4, y, w, h);
                }
            }
        }

    }

    static final class Loading extends DefaultTableCellRenderer {

        private final BusyIndicator over = new BusyIndicator();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(null); //!
            label.setText(null);

            // https://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof TimelineLayer layer && layer.isDownloading()) {
                Rectangle rect = table.getCellRect(row, column, false);
                table.repaint(rect.x, rect.y, rect.width, rect.height);

                over.setForeground(label.getForeground());
                over.setBackground(label.getBackground());
                over.setOpaque(label.isOpaque());
                return over;
            }
            return label;
        }

    }

    static final class Name extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // label.setText(null);

            // https://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof TimelineLayer layer) {
                String layerName = layer.getName();
                if (layer.hasData()) {
                    label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                    label.setToolTipText(layerName);
                } else {
                    label.setForeground(Color.GRAY);
                    label.setToolTipText(layerName + ": No data for selected interval");
                }
                label.setText(layerName);
            }
            return label;
        }

    }

    static final class Remove extends DefaultTableCellRenderer {

        private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

        @Override
        public void setValue(Object value) {
            setBorder(null); //!
            if (value instanceof TimelineLayer layer && layer.isDeletable()) {
                setFont(font);
                setText(Buttons.close);
            } else
                setText(null);
        }

    }

}
