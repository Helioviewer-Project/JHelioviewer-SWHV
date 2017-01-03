package org.helioviewer.jhv.gui.components.base;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;

@SuppressWarnings({ "serial", "rawtypes"})
public class BusyIndicator extends LayerUI<JComponent> implements ActionListener {

    private int angle;

    private static int brightness(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int cmax = Math.max(Math.max(r, g), b);
        int cmin = Math.min(Math.min(r, g), b);
        return (cmax + cmin) / 2;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = c.getWidth();
        int h = c.getHeight();
        double s = Math.min(w, h) / 4.;
        double cx = w / 2.;
        double cy = h / 2.;

        g2.setStroke(new BasicStroke((float) (s / 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.rotate(angle * (Math.PI / 180), cx, cy);

        if (brightness(c.getBackground()) > 127)
            g2.setPaint(Color.black);
        else
            g2.setPaint(Color.white);

        Line2D line = new Line2D.Double(cx + s, cy, cx + s * 2, cy);
        for (int i = 0; i < 12; i++) {
            g2.draw(line);
            g2.rotate(-Math.PI / 6, cx, cy);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (11 - i) / 11f));
        }

        g2.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        firePropertyChange("tick", 0, 1);
        angle += 6;
        if (angle >= 360)
            angle = 0;
    }

    @Override
    public void applyPropertyChange(PropertyChangeEvent pce, JLayer l) {
        if ("tick".equals(pce.getPropertyName())) {
            l.repaint();
        }
    }

}
