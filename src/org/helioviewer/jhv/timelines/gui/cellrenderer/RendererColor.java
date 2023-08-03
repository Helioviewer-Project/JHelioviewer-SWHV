package org.helioviewer.jhv.timelines.gui.cellrenderer;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public final class RendererColor extends DefaultTableCellRenderer {

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
