package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;

import javax.annotation.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.component.BusyIndicator;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.view.View;

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
                    // Repaint the whole row (not just the spinner cell) each animation tick so the
                    // finished/total count in the Time column keeps up as frames stream in.
                    Rectangle rect = table.getCellRect(row, column, false);
                    table.repaint(0, rect.y, table.getWidth(), rect.height); // lazy

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

        // "2026-07-08T03:43:39.715" -> "03:43:39"
        @Nullable
        private static String shortTime(@Nullable String timeString) {
            if (timeString == null)
                return null;
            int t = timeString.indexOf('T');
            if (t < 0)
                return timeString;
            int dot = timeString.indexOf('.', t);
            return timeString.substring(t + 1, dot > t ? dot : timeString.length());
        }

        @Override
        public void setValue(Object value) {
            if (value instanceof Layer layer) {
                setFont(font);
                // While downloading, append finished/total to the right of the timestamp (still
                // just left of the row's download spinner) rather than replacing the time.
                if (layer.isDownloading() && layer instanceof ImageLayer il) {
                    View view = il.getView();
                    int max = view.getMaximumFrameNumber();
                    // Before the download scope is known (max == 0) show 0 / 0 rather than 1 / 1.
                    String count = max == 0 ? "0 / 0" : view.getCompleteFrameCount() + " / " + (max + 1);
                    // Compact time-of-day (drop date + millis) so the count fits without widening
                    // the column; the full timestamp returns once the download completes.
                    String time = shortTime(layer.getTimeString());
                    setText(time == null ? count : time + "   " + count);
                } else {
                    setText(layer.getTimeString());
                }
            }
        }

    }

}
