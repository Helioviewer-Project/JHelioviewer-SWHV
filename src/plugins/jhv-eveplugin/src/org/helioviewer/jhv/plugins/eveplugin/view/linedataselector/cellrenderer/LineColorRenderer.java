package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class LineColorRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null) {
            JLabel p = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof LineDataSelectorElement) {
                LineDataSelectorElement ldse = (LineDataSelectorElement) value;
                if (ldse.getDataColor() != null) {
                    LineColorPanel lineColorPanel = new LineColorPanel(ldse.getDataColor());
                    lineColorPanel.setBackground(p.getBackground());
                    lineColorPanel.setBorder(LineDateSelectorTablePanel.commonBorder);
                    return lineColorPanel;
                } else {
                    p.setBorder(LineDateSelectorTablePanel.commonBorder);
                    p.setText(null);
                    return p;
                }
            }
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public class LineColorPanel extends JPanel {
        private final Color c;

        public LineColorPanel(Color c) {
            this.c = c;
            this.setBackground(super.getBackground());
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(c);
            g.fillRect(0, getHeight() / 2 - 1, getWidth(), 2);
        }
    }

}
