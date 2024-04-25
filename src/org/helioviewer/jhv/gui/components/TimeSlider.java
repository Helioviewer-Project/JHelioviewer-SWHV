package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.view.View;

// Extension of JSlider displaying the caching status on the track.
// This element provides its own look and feel. Therefore, it is independent
// of the global look and feel.
@SuppressWarnings("serial")
public final class TimeSlider extends JSlider implements LazyComponent, MouseListener, MouseMotionListener, MouseWheelListener {

    private final TimeSliderUI sliderUI;
    private final JLabel frameNumberLabel;
    private boolean dirty;
    private boolean wasPlaying;
    private boolean allowSetFrame;

    public TimeSlider(int _orientation, int min, int max, int value) {
        super(_orientation, min, max, value);
        setSnapToTicks(true);

        sliderUI = new TimeSliderUI(this);
        setUI(sliderUI);
        // remove all mouse listeners installed by BasicSliderUI.TrackListener
        for (MouseListener l : getMouseListeners())
            removeMouseListener(l);
        for (MouseMotionListener l : getMouseMotionListeners())
            removeMouseMotionListener(l);
        for (MouseWheelListener l : getMouseWheelListeners())
            removeMouseWheelListener(l);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        UITimer.register(this);

        frameNumberLabel = new JLabel((getValue() + 1) + "/" + (getMaximum() + 1), JLabel.RIGHT);
    }

    JLabel getFrameNumberPanel() {
        return frameNumberLabel;
    }

    void setAllowFrame(boolean _allowSetFrame) {
        allowSetFrame = _allowSetFrame;
    }

    // Overrides updateUI, to keep own SliderUI
    @Override
    public void updateUI() {
    }

    @Override
    public void repaint() {
        dirty = true;
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        dirty = true;
    }

    @Override
    public void lazyRepaint() {
        if (dirty) {
            super.repaint();
            frameNumberLabel.setText((getValue() + 1) + "/" + (getMaximum() + 1));
            dirty = false;
        }
    }

    @Override
    public void setValue(int n) {
        super.setValue(n);
        if (allowSetFrame)
            Movie.setFrame(n);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0)
            Movie.nextFrame();
        else if (e.getWheelRotation() > 0)
            Movie.previousFrame();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setValue(sliderUI.valueForXPosition(e.getX()));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        wasPlaying = Movie.isPlaying();
        if (wasPlaying)
            Movie.pause();
        mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (wasPlaying)
            Movie.play();
    }

    // Extension of BasicSliderUI overriding some drawing functions.
    // All functions for size calculations stay the same.
    private static class TimeSliderUI extends BasicSliderUI {

        private static final Color completeColor = UIManager.getColor("Table.selectionBackground");
        private static final Color partialColor = completeColor.darker();
        private static final Color emptyColor = UIManager.getColor("ProgressBar.background");

        private static final BasicStroke thinStroke = new BasicStroke(1);
        private static final BasicStroke thickStroke = new BasicStroke(4);

        TimeSliderUI(JSlider component) {
            super(component);
        }

        @Override
        public void paintThumb(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setStroke(thinStroke);
            g.setColor(UIGlobals.foreColor);
            g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);

            int x = thumbRect.x + (thumbRect.width - 1) / 2;
            g.drawLine(x, thumbRect.y, x, thumbRect.y + thumbRect.height - 1);
        }

        // Draws the different regions: no/partial/complete information
        @Override
        public void paintTrack(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setStroke(thickStroke);

            int y = slider.getSize().height / 2;
            View view;
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer == null) {
                g.setColor(emptyColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else if ((view = layer.getView()).isComplete()) {
                g.setColor(completeColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else {
                int len = view.getMaximumFrameNumber();
                for (int i = 0; i < len; i++) {
                    int begin = (int) ((float) i / len * trackRect.width);
                    int end = (int) ((float) (i + 1) / len * trackRect.width);
                    if (end == begin)
                        end++;

                    AtomicBoolean status = view.getFrameCompletion(i);
                    g.setColor(status == null ? emptyColor : (status.get() ? completeColor : partialColor));
                    g.drawLine(trackRect.x + begin, y, trackRect.x + end, y);
                }
            }
        }

    }

}
