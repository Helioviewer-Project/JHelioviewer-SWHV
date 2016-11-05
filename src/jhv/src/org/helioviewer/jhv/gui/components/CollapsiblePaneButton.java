package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public class CollapsiblePaneButton extends JToggleButton {

    private final static Color color = new JPanel().getBackground();
    private final Color bright;
    private final Color dark;

    public CollapsiblePaneButton(String title) {
        super(title);
        setContentAreaFilled(false);
        setFocusPainted(false); // used for demonstration

        bright = brighter(color, 0.85);
        dark = darker(color, 0.9);
    }

    private Color brighter(Color c, double FACTOR) {
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

    private Color darker(Color c, double FACTOR) {
        return new Color(Math.max((int) (c.getRed() * FACTOR), 0), Math.max((int) (c.getGreen() * FACTOR), 0), Math.max((int) (c.getBlue() * FACTOR), 0), c.getAlpha());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();
        Point p0 = new Point(0, 0);
        Point ph = new Point(0, h);

        if (!isSelected()) {
            g2.setPaint(new GradientPaint(p0, dark, ph, color));
            g2.fillRect(0, 0, w, h / 2);
            g2.setPaint(new GradientPaint(p0, color, ph, dark));
            g2.fillRect(0, h / 2, w, h / 2);
        } else {
            g2.setPaint(new GradientPaint(p0, dark, ph, bright));
            g2.fillRect(0, 0, w, h / 2);
            g2.setPaint(new GradientPaint(p0, bright, ph, dark));
            g2.fillRect(0, h / 2, w, h / 2);
        }
        g2.dispose();

        super.paintComponent(g);
    }

}
