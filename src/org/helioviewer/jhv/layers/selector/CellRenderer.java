package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.gui.components.Buttons;
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

    static final class Loading extends DefaultTableCellRenderer {

        private final Font font = Buttons.getMaterialFont(getFont().getSize2D());
        private final BusyIndicator over = new BusyIndicator();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(null); //!
            label.setText(null);

            // https://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof Layer layer) {
                if (layer.isDownloading()) {
                    Rectangle rect = table.getCellRect(row, column, false);
                    table.repaint(rect.x, rect.y, rect.width, rect.height);

                    over.setForeground(label.getForeground());
                    over.setBackground(label.getBackground());
                    over.setOpaque(label.isOpaque());
                    return over;
                } else if (layer.isLocal()) {
                    label.setFont(font);
                    label.setText(Buttons.check);
                }
            }
            return label;
        }

    }

    static final class Name extends DefaultTableCellRenderer {

        @Override
        public void setValue(Object value) {
            if (value instanceof Layer layer) {
                String layerName = layer.getName();
                setText(layerName);
                if (layer == Layers.getActiveImageLayer()) {
                    setToolTipText(layerName + " (master)");
                    setFont(UIGlobals.uiFontBold);
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
