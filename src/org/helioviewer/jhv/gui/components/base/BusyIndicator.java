package org.helioviewer.jhv.gui.components.base;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;

@SuppressWarnings("serial")
public class BusyIndicator extends LayerUI<JComponent> {

    @Override
    public void paint(Graphics g1, JComponent c) {
        super.paint(g1, c);

        int w = c.getWidth();
        int h = c.getHeight();
        double s = Math.min(w, h) / 4.;
        if (s < 1)
            return;
        double cx = w / 2.;
        double cy = h / 2.;

        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke((float) (s / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.rotate(angle, cx, cy);
        g.setColor(c.getForeground());

        Line2D line = new Line2D.Double(cx + s, cy, cx + s * 2, cy);
        for (int i = 0; i < 12; i++) {
            g.draw(line);
            g.rotate(-Math.PI / 6, cx, cy);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (11 - i) / 11f));
        }
    }

    private static final double dangle = Math.PI / 30;
    private static double angle;

    public static void incrementAngle() {
        angle += dangle;
        if (angle > 2 * Math.PI)
            angle = 0;
    }

}
