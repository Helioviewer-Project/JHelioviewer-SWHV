package org.helioviewer.jhv.gui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.UIGlobals;

@SuppressWarnings("serial")
public final class CollapsiblePaneButton extends JToggleButton {

    private static final Color bright = brighter(UIGlobals.backColor, 0.85);
    private static final Color dark = darker(UIGlobals.backColor, 0.9);
    private static final int THICKNESS_HORZ = 14;
    private static final int THICKNESS_VERT = 12;

    private final boolean vertical;

    public CollapsiblePaneButton() {
        this(SwingConstants.HORIZONTAL);
    }

    public CollapsiblePaneButton(int orientation) {
        vertical = orientation == SwingConstants.VERTICAL;
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (vertical) {
            size.width = THICKNESS_VERT;
        } else {
            size.height = THICKNESS_HORZ;
        }
        return size;
    }

    private static Color brighter(Color c, double FACTOR) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int alpha = c.getAlpha();

        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i)
            r = i;
        if (g > 0 && g < i)
            g = i;
        if (b > 0 && b < i)
            b = i;

        return new Color(Math.min((int) (r / FACTOR), 255), Math.min((int) (g / FACTOR), 255), Math.min((int) (b / FACTOR), 255), alpha);
    }

    private static Color darker(Color c, double FACTOR) {
        return new Color(Math.max((int) (c.getRed() * FACTOR), 0), Math.max((int) (c.getGreen() * FACTOR), 0), Math.max((int) (c.getBlue() * FACTOR), 0), c.getAlpha());
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1.create();
        int w = getWidth();
        int h = getHeight();
        Point p0 = new Point(0, 0);
        Point ph = vertical ? new Point(w, 0) : new Point(0, h);
        int half = vertical ? w / 2 : h / 2;

        if (isSelected()) {
            g.setPaint(new GradientPaint(p0, bright, ph, dark));
            g.fillRect(0, 0, vertical ? half : w, vertical ? h : half);
            g.setPaint(new GradientPaint(p0, dark, ph, bright));
            g.fillRect(vertical ? half : 0, vertical ? 0 : half, vertical ? half : w, vertical ? h : half);
        } else {
            g.setPaint(new GradientPaint(p0, dark, ph, bright));
            g.fillRect(0, 0, vertical ? half : w, vertical ? h : half);
            g.setPaint(new GradientPaint(p0, bright, ph, dark));
            g.fillRect(vertical ? half : 0, vertical ? 0 : half, vertical ? half : w, vertical ? h : half);
        }
        g.dispose();

        super.paintComponent(g1);
    }

}
