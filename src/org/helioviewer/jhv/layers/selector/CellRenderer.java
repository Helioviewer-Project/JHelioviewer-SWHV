package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
class CellRenderer {

    static final class Enabled extends DefaultTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof Layer layer) {
                checkBox.setSelected(layer.isEnabled());
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    static final class Loading extends DefaultTableCellRenderer {

        private final Font font = Buttons.getMaterialFont(getFont().getSize2D());
        private final JLayer<JComponent> over = new JLayer<>(null, UITimer.busyIndicator);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(null); //!
            label.setText(null);

            // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
            if (value instanceof Layer layer) {
                if (layer.isDownloading()) {
                    table.repaint(); // lazy

                    over.setForeground(label.getForeground());
                    over.setView(label);
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

        @Override
        public void setValue(Object value) {
            if (value instanceof Layer layer) {
                setText(layer.getTimeString());
            }
        }

    }

}
