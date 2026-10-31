package org.helioviewer.jhv.gui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.ui.Panel;

// Extension of JSlider displaying the caching status on the track.
// This element provides its own look and feel. Therefore, it is independent
// of the global look and feel.
@SuppressWarnings("serial")
public final class TimeSlider extends JSlider implements Interfaces.LazyComponent, MouseListener, MouseMotionListener, MouseWheelListener {

    // What the user did; the model comes back through update().
    public interface Target {
        void seek(int frame);

        void range(int first, int last);

        void command(Panel.Command command);
    }

    private enum DragMode {
        Frame, Range, RangeStart, RangeEnd
    }

    private static final int RANGE_MARKER_SIZE = 6;
    private static final int MENU_SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Cmd on macOS, Ctrl elsewhere

    private final Target target;
    private final TimeSliderUI sliderUI;
    private boolean dirty;
    private int first;
    private int last;
    private byte[] completion = new byte[0];
    private int dragAnchorValue;
    private int dragRangeMin;
    private int dragRangeMax;
    private DragMode dragMode = DragMode.Frame;

    public TimeSlider(Target _target) {
        super(HORIZONTAL, 0, 0, 0);
        target = _target;
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

        bindKey(KeyEvent.VK_RIGHT, "nextFrame", Panel.Command.STEP_FORWARD);
        bindKey(KeyEvent.VK_LEFT, "previousFrame", Panel.Command.STEP_BACKWARD);
        bindKey(KeyEvent.VK_SPACE, "playPause", Panel.Command.PLAY_PAUSE);
    }

    private void bindKey(int key, String name, Panel.Command command) {
        getInputMap().put(KeyStroke.getKeyStroke(key, 0, false), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                target.command(command);
            }
        });
    }

    // Programmatic: reports nothing back.
    void update(int frame, int _first, int _last, int maximum, byte[] _completion) {
        if (first != _first || last != _last || !Arrays.equals(completion, _completion)) {
            first = _first;
            last = _last;
            completion = _completion;
            repaint();
        }
        getModel().setRangeProperties(frame, 0, 0, maximum, false);
    }

    private void setRange(int min, int max) {
        target.range(
                Math.clamp(Math.min(min, max), getMinimum(), getMaximum()),
                Math.clamp(Math.max(min, max), getMinimum(), getMaximum()));
    }

    // Overrides updateUI, to keep own SliderUI
    @Override
    public void updateUI() {}

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
            dirty = false;
        }
    }

    @Override
    public void setValue(int n) {
        super.setValue(n);
        target.seek(n);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0)
            target.command(Panel.Command.INCREASE);
        else if (e.getWheelRotation() > 0)
            target.command(Panel.Command.DECREASE);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setCursor(cursorFor(dragMode));
        int value = sliderUI.valueForXPosition(e.getX());
        switch (dragMode) {
            case Frame -> setValue(Math.clamp(value, first, last));
            case Range -> dragRange(value);
            case RangeStart -> setRange(value, last);
            case RangeEnd -> setRange(first, value);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setCursor(cursorFor(e));
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        target.command(Panel.Command.SCRUB_START);
        dragMode = dragModeFor(e);
        dragAnchorValue = sliderUI.valueForXPosition(e.getX());
        dragRangeMin = first;
        dragRangeMax = last;
        mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragMode = DragMode.Frame;
        setCursor(cursorFor(e));
        target.command(Panel.Command.SCRUB_END);
    }

    private void dragRange(int value) {
        int delta = value - dragAnchorValue;
        int width = dragRangeMax - dragRangeMin;
        int min = getMinimum();
        int max = getMaximum();
        int newMin = dragRangeMin + delta;
        int newMax = dragRangeMax + delta;

        if (newMin < min) {
            newMin = min;
            newMax = min + width;
        } else if (newMax > max) {
            newMax = max;
            newMin = max - width;
        }

        setRange(newMin, newMax);
    }

    private DragMode dragModeFor(MouseEvent e) {
        if ((e.getModifiersEx() & MENU_SHORTCUT_MASK) != 0)
            return DragMode.Range;
        if (e.isAltDown())
            return nearestBoundary(e.getX());
        return DragMode.Frame;
    }

    private Cursor cursorFor(MouseEvent e) {
        return cursorFor(dragModeFor(e));
    }

    private static Cursor cursorFor(DragMode dragMode) {
        return switch (dragMode) {
            case Range -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case RangeStart -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case RangeEnd -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case Frame -> Cursor.getDefaultCursor();
        };
    }

    private DragMode nearestBoundary(int x) {
        int rangeStartX = sliderUI.xPosition(first);
        int rangeEndX = sliderUI.xPosition(last);
        return Math.abs(x - rangeStartX) <= Math.abs(x - rangeEndX) ? DragMode.RangeStart : DragMode.RangeEnd;
    }

    // Extension of BasicSliderUI overriding some drawing functions.
    // All functions for size calculations stay the same.
    private static class TimeSliderUI extends BasicSliderUI {

        private static final Color completeColor = UIManager.getColor("Table.selectionBackground");
        private static final Color partialColor = completeColor.darker();
        private static final Color emptyColor = UIManager.getColor("ProgressBar.background");
        private static final Color rangeColor = new Color(completeColor.getRed(), completeColor.getGreen(), completeColor.getBlue(), 96);

        private static final BasicStroke thinStroke = new BasicStroke(1);
        private static final BasicStroke thickStroke = new BasicStroke(4);

        private final TimeSlider timeSlider;

        TimeSliderUI(TimeSlider component) {
            super(component);
            timeSlider = component;
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

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            paintRangeMarkers((Graphics2D) g);
        }

        // Draws the different regions: no/partial/complete information
        @Override
        public void paintTrack(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setStroke(thickStroke);

            int y = slider.getSize().height / 2;
            byte[] completion = timeSlider.completion;
            int len = completion.length;
            boolean complete = true;
            for (byte status : completion)
                complete &= status == 2;
            if (complete) {
                g.setColor(completeColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else {
                for (int i = 0; i < len; i++) {
                    int begin = (int) ((float) i / len * trackRect.width);
                    int end = (int) ((float) (i + 1) / len * trackRect.width);
                    if (end == begin)
                        end++;

                    g.setColor(completion[i] == 0 ? emptyColor : (completion[i] == 2 ? completeColor : partialColor));
                    g.drawLine(trackRect.x + begin, y, trackRect.x + end, y);
                }
            }

            int rangeStartX = xPosition(timeSlider.first);
            int rangeEndX = xPosition(timeSlider.last);
            int left = Math.min(rangeStartX, rangeEndX);
            int right = Math.max(rangeStartX, rangeEndX);
            int width = Math.max(1, right - left + 1);
            g.setColor(rangeColor);
            g.fillRect(left, trackRect.y, width, trackRect.height);
        }

        private void paintRangeMarkers(Graphics2D g) {
            int rangeStartX = xPosition(timeSlider.first);
            int rangeEndX = xPosition(timeSlider.last);
            int left = Math.min(rangeStartX, rangeEndX);
            int right = Math.max(rangeStartX, rangeEndX);
            int markerY = trackRect.y + trackRect.height - 1;

            g.setColor(UIGlobals.foreColor);
            g.fillPolygon(upTriangle(left, markerY));
            g.fillPolygon(upTriangle(right, markerY));
        }

        private static Polygon upTriangle(int x, int y) {
            int half = RANGE_MARKER_SIZE / 2;
            return new Polygon(
                    new int[]{x - half, x + half, x},
                    new int[]{y, y, y - RANGE_MARKER_SIZE},
                    3);
        }

        int xPosition(int value) {
            return xPositionForValue(value);
        }

    }

}
