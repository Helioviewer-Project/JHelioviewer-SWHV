package org.helioviewer.jhv.gui.components.base;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class BusyIndicator extends JComponent {

    private static final AlphaComposite[] alphas = new AlphaComposite[12];
    private static final double dangle = Math.PI / 30;
    private static double angle;

    static {
        for (int i = 0; i < alphas.length; i++)
            alphas[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (11 - i) / 11f);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        if (isOpaque()) {
            g1.setColor(getBackground());
            g1.fillRect(0, 0, getWidth(), getHeight());
        }

        int w = getWidth();
        int h = getHeight();
        double s = Math.min(w, h) / 4.;
        if (s < 1)
            return;
        double cx = w / 2.;
        double cy = h / 2.;

        Graphics2D g = (Graphics2D) g1.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke((float) (s / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.rotate(angle, cx, cy);
        g.setColor(getForeground());

        Line2D line = new Line2D.Double(cx + s, cy, cx + s * 2, cy);
        for (int i = 0; i < 12; i++) {
            g.draw(line);
            g.rotate(-Math.PI / 6, cx, cy);
            g.setComposite(alphas[i]);
        }
        g.dispose();
    }

    public static void incrementAngle() {
        angle = (angle + dangle) % (2 * Math.PI);
    }

}
