package org.helioviewer.jhv.gui.components.base;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class CircularProgressUI extends BasicProgressBarUI {

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        int v = Math.max(d.width, d.height);
        d.setSize(v, v);
        return d;
    }

    @Override
    protected Rectangle getBox(Rectangle r) {
        if (r != null) {
            r.setBounds(progressBar.getBounds());
            return r;
        }
        return progressBar.getBounds();
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        doPaint((Graphics2D) g, 360. * progressBar.getPercentComplete());
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        doPaint((Graphics2D) g, 360. * getAnimationIndex() / getFrameCount());
    }

    private static final double THICK_FACTOR = 1 / 8.;

    private void doPaint(Graphics2D g, double degree) {
        Rectangle r = progressBar.getBounds();
        double sz = Math.min(r.width, r.height) * (1 - 3 * THICK_FACTOR);
        double cx = r.width * .5;
        double cy = r.height * .5;
        double or = sz * .5;
        double ir = or * .5;

        g.setPaint(progressBar.getForeground());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D in = new Rectangle2D.Double(cx - ir, cy - ir, or, or);
        g.fill(in);

        Arc2D out = new Arc2D.Double(cx - or, cy - or, sz, sz, 90 - degree, degree, Arc2D.OPEN);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke((float) (sz * THICK_FACTOR), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(out);
    }

}
