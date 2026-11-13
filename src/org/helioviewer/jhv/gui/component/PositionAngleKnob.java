package org.helioviewer.jhv.gui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.function.DoubleConsumer;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

// A position angle, north up, counterclockwise: point at it, scroll, or use the arrow keys;
// the value in the middle, a dot on the rim.
@SuppressWarnings({"serial", "this-escape"})
public final class PositionAngleKnob extends JComponent {

    private static final String WIDEST = "000°";

    private final DoubleConsumer setter;
    private int degrees;

    public PositionAngleKnob(double initial, DoubleConsumer _setter) {
        setter = _setter;
        degrees = Math.floorMod(Math.round(initial), 360);
        int defaultDegrees = degrees;
        setFocusable(true);
        setToolTipText("Position angle");

        MouseInputAdapter mouse = new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled() && e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    set(defaultDegrees);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                point(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                point(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(e -> set(degrees - e.getWheelRotation()));
        bind("UP", 1);
        bind("RIGHT", 1);
        bind("DOWN", -1);
        bind("LEFT", -1);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    private void bind(String key, int step) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                set(degrees + step);
            }
        });
    }

    // Position angle of the mouse around the center.
    private void point(MouseEvent e) {
        set((int) Math.round(Math.toDegrees(Math.atan2(getWidth() / 2. - e.getX(), getHeight() / 2. - e.getY()))));
    }

    private void set(int value) {
        int d = Math.floorMod(value, 360);
        if (d != degrees) {
            degrees = d;
            setter.accept(d);
            repaint();
        }
    }

    private static Font font() {
        return UIManager.getFont("Label.font");
    }

    private static double dotRadius(FontMetrics metrics) {
        return metrics.getHeight() / 6.;
    }

    // Room for the widest value inside the path of the dot.
    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(font());
        int size = (int) Math.ceil(metrics.stringWidth(WIDEST) + 4 * dotRadius(metrics) + 4);
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    // Text beside the knob centers on it.
    @Override
    public int getBaseline(int width, int height) {
        FontMetrics metrics = getFontMetrics(font());
        return (height + metrics.getAscent() - metrics.getDescent()) / 2;
    }

    @Override
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.CENTER_OFFSET;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = font();
        FontMetrics metrics = g2.getFontMetrics(font);
        double dot = dotRadius(metrics);
        double cx = getWidth() / 2., cy = getHeight() / 2., r = Math.min(cx, cy) - dot - 1;

        Ellipse2D circle = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        g2.setColor(color("Button.background"));
        g2.fill(circle);
        g2.setColor(color("Component.borderColor"));
        g2.draw(new Line2D.Double(cx, cy - r, cx, cy - r + 2 * dot)); // north
        if (isFocusOwner()) {
            g2.setColor(color("Component.focusColor"));
            g2.setStroke(new BasicStroke(2));
        }
        g2.draw(circle);

        String text = degrees + "°";
        g2.setColor(color("Label.foreground"));
        g2.setFont(font);
        double right = cx + metrics.stringWidth(WIDEST) / 2.; // the digits stay put as the value changes
        g2.drawString(text, (float) (right - metrics.stringWidth(text)), (float) (cy + (metrics.getAscent() - metrics.getDescent()) / 2.));

        double theta = Math.toRadians(degrees);
        double x = cx - r * Math.sin(theta), y = cy - r * Math.cos(theta);
        g2.setColor(color("Slider.thumbColor"));
        g2.fill(new Ellipse2D.Double(x - dot, y - dot, 2 * dot, 2 * dot));
        g2.dispose();
    }

    private Color color(String key) {
        Color color = UIManager.getColor(key);
        return color == null ? getForeground() : color;
    }

}
