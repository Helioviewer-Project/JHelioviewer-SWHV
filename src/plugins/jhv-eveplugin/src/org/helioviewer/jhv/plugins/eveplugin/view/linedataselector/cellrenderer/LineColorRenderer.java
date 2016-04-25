package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineColorRenderer extends DefaultTableCellRenderer {

    private final LineColorPanel lineColorPanel = new LineColorPanel();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement ldse = (LineDataSelectorElement) value;

            Color c = ldse.getDataColor();
            if (c != null) {
                lineColorPanel.setLineColor(c);
                lineColorPanel.setBackground(label.getBackground());
                lineColorPanel.setBorder(LineDataSelectorTablePanel.commonBorder);
                return lineColorPanel;
            }

            label.setText(null);
            label.setBorder(LineDataSelectorTablePanel.commonBorder);
        }

        return label;
    }

    private static class LineColorPanel extends JPanel {

        private Color c;

        public void setLineColor(Color c) {
            this.c = c;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(c);
            g.fillRect(0, getHeight() / 2 - 1, getWidth(), 2);
        }

    }

}
