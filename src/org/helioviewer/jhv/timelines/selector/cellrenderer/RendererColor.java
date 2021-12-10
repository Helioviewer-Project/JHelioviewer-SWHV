package org.helioviewer.jhv.timelines.selector.cellrenderer;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public class RendererColor extends DefaultTableCellRenderer {

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
            g.fillRect(0, getHeight() / 2 - 1, getWidth(), 2);
        }
    }

}
