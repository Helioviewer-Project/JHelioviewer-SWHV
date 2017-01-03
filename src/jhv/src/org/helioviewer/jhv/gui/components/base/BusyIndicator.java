package org.helioviewer.jhv.gui.components.base;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import javax.swing.JLabel;

public class BusyIndicator {

    private static int brightness(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int cmax = Math.max(Math.max(r, g), b);
        int cmin = Math.min(Math.min(r, g), b);
        return (cmax + cmin) / 2;
    }

    @SuppressWarnings("serial")
    public static class BusyLabel extends JLabel {

        @Override
        public void paint(Graphics g1) {
            Graphics2D g = (Graphics2D) g1.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();

            Color c = getBackground();
            g.setPaint(c);
            g.fillRect(0, 0, w, h);

            double s = Math.min(w, h) / 4.;
            double cx = w / 2.;
            double cy = h / 2.;

            g.setStroke(new BasicStroke((float) (s / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.rotate(angle, cx, cy);

            if (brightness(c) > 127)
                g.setPaint(Color.black);
            else
                g.setPaint(Color.white);

            Line2D line = new Line2D.Double(cx + s, cy, cx + s * 2, cy);
            for (int i = 0; i < 12; i++) {
                g.draw(line);
                g.rotate(-Math.PI / 6, cx, cy);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (11 - i) / 11f));
            }

            g.dispose();

            paintBorder(g1);
        }

    }

    private static final double dangle = Math.PI / 30;
    private static double angle;

    public static void incrementAngle() {
        angle += dangle;
    }

}
