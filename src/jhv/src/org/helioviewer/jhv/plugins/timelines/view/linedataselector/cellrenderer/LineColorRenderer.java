package org.helioviewer.jhv.plugins.timelines.view.linedataselector.cellrenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineColorRenderer extends DefaultTableCellRenderer {

    private final LineColorPanel lineColorPanel = new LineColorPanel();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Color back = isSelected ? table.getSelectionBackground() : table.getBackground();
        lineColorPanel.setLineColor(back);
        lineColorPanel.setBackground(back);

        // http://stackoverflow.com/questions/3054775/jtable-strange-behavior-from-getaccessiblechild-method-resulting-in-null-point
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement ldse = (LineDataSelectorElement) value;
            if (ldse.hasDataColor()) {
                lineColorPanel.setLineColor(ldse.getDataColor());
            }
        }
        return lineColorPanel;
    }

    private static class LineColorPanel extends JPanel {

        private Color c;

        public LineColorPanel() {
            setBorder(LineDataSelectorTablePanel.commonBorder);
        }

        public void setLineColor(Color _c) {
            c = _c;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (c != null) {
                g.setColor(c);
                g.fillRect(0, getHeight() / 2 - 1, getWidth(), 2);
            }
        }

    }

}
