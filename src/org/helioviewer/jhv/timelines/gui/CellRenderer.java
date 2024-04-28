package org.helioviewer.jhv.timelines.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
class CellRenderer {

    static final class LineColor extends DefaultTableCellRenderer {

        private Color c;

        @Override
        public void setValue(Object value) {
            if (value instanceof TimelineLayer layer) {
                c = layer.getDataColor();
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (c != null) {
                g.setColor(c);
                g.fillRect(4, getHeight() / 2 - 1, getWidth() - 4, 2);
            }
        }

    }

    static final class Enabled extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof TimelineLayer layer) {
                checkBox.setSelected(layer.isEnabled());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    static final class Loading extends DefaultTableCellRenderer {

        private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(null); //!
            label.setText(null);

            // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof TimelineLayer layer && layer.isDownloading()) {
                table.repaint(); // lazy

                over.setForeground(label.getForeground());
                over.setView(label);
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

            // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
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
