package org.helioviewer.jhv.gui.components.base;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
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
        doPaint(g, c, 360. * progressBar.getPercentComplete());
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        doPaint(g, c, 360. * getAnimationIndex() / getFrameCount());
    }

    private static final double THICK_FACTOR = 1 / 6.;

    private void doPaint(Graphics g1, JComponent c, double degree) {
        Insets b = progressBar.getInsets();
        int barRectWidth  = progressBar.getWidth()  - b.right - b.left;
        int barRectHeight = progressBar.getHeight() - b.top - b.bottom;
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g1.create();
        g.setPaint(progressBar.getForeground());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double sz = Math.min(barRectWidth, barRectHeight) * (1 - THICK_FACTOR);
        double cx = b.left + barRectWidth  * .5;
        double cy = b.top  + barRectHeight * .5;
        double or = sz * .5;
        double ir = or * .5;

        //Rectangle2D in = new Rectangle2D.Double(cx - ir, cy - ir, or, or);
        //g.fill(in);

        Arc2D out = new Arc2D.Double(cx - or, cy - or, sz, sz, 90 - degree, degree, Arc2D.OPEN);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setStroke(new BasicStroke((float) (sz * THICK_FACTOR), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(out);
        g.dispose();

        if (progressBar.isStringPainted()) {
            paintString(g1, b.left, b.top, barRectWidth, barRectHeight, 0, b);
        }
    }

}
