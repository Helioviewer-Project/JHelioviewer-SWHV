package org.helioviewer.jhv.gui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.view.View;

// Extension of JSlider displaying the caching status on the track.
// This element provides its own look and feel. Therefore, it is independent
// of the global look and feel.
@SuppressWarnings("serial")
public final class TimeSlider extends JSlider implements Interfaces.LazyComponent, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, Player.Listener, Player.StatusListener, ViewState.PlaybackRangeListener {

    private enum DragMode {
        Frame, Range, RangeStart, RangeEnd
    }

    private static final int RANGE_MARKER_SIZE = 6;
    private static final int MENU_SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Cmd on macOS, Ctrl elsewhere
    // Human names for the range-gesture modifiers, so the tooltip documents the otherwise-hidden
    // trim/move gestures in the user's own platform vocabulary.
    private static final String ALT_KEY = Platform.isMacOS() ? "⌥ Option" : "Alt";
    private static final String MENU_KEY = Platform.isMacOS() ? "⌘ Cmd" : "Ctrl";

    private final TimeSliderUI sliderUI;
    private final FrameNumberPanel frameNumberPanel;
    private boolean dirty;
    private boolean wasPlaying;
    private boolean allowSetFrame = true;
    private int dragAnchorValue;
    private int dragRangeMin;
    private int dragRangeMax;
    private DragMode dragMode = DragMode.Frame;
    private boolean dragging; // true between press and release, so the move gesture can close the hand
    private boolean hovering;  // mouse is over the track, so key press/release should refresh the cursor
    private int lastX;         // last hover x, so a modifier keypress can recompute the cursor in place

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
        addKeyListener(this);
        Player.addFrameListener(this);
        Player.addStatusListener(this);
        UITimer.register(this);
        ViewState.addPlaybackRangeListener(this);

        // Register with the tooltip manager; getToolTipText(MouseEvent) fills in the actual text.
        // The trim/move gestures are otherwise invisible, so this is their in-place documentation.
        setToolTipText("");

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "nextFrame");
        getActionMap().put("nextFrame", Actions.NEXT_FRAME);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "previousFrame");
        getActionMap().put("previousFrame", Actions.PREVIOUS_FRAME);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "playPause");
        getActionMap().put("playPause", Actions.PLAY_PAUSE);

        frameNumberPanel = new FrameNumberPanel(getValue(), getMaximum());
    }

    JComponent getFrameNumberPanel() {
        return frameNumberPanel;
    }

    void setAllowFrame(boolean _allowSetFrame) {
        allowSetFrame = _allowSetFrame;
    }

    @Override
    public void setMaximum(int maximum) {
        super.setMaximum(maximum);
        if (frameNumberPanel != null)
            frameNumberPanel.setFrame(getValue(), maximum);
        repaint();
    }

    private void setRange(int min, int max) {
        Commands.setPlaybackRange(
                Math.clamp(Math.min(min, max), getMinimum(), getMaximum()),
                Math.clamp(Math.max(min, max), getMinimum(), getMaximum()));
        repaint();
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
            frameNumberPanel.setFrame(getValue(), getMaximum());
            dirty = false;
        }
    }

    @Override
    public void setValue(int n) {
        super.setValue(n);
        if (allowSetFrame)
            Commands.seekFrame(n);
    }

    @Override
    public void frameChanged(int frame, boolean last) {
        setAllowFrame(false);
        setValue(frame);
        setAllowFrame(true);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0)
            Commands.nextFrame();
        else if (e.getWheelRotation() > 0)
            Commands.previousFrame();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setCursor(cursorFor(dragMode));
        int value = sliderUI.valueForXPosition(e.getX());
        switch (dragMode) {
            case Frame -> setValue(Math.clamp(value, getPlaybackFirstFrame(), getPlaybackLastFrame()));
            case Range -> dragRange(value);
            case RangeStart -> setRange(value, getPlaybackLastFrame());
            case RangeEnd -> setRange(getPlaybackFirstFrame(), value);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastX = e.getX();
        setCursor(cursorFor(e));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        refreshHoverCursor(e.getModifiersEx());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        refreshHoverCursor(e.getModifiersEx());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Recompute the cursor from the live modifier state while hovering, so pressing or releasing
    // Option / Cmd updates it in place — no mouse move required.
    private void refreshHoverCursor(int modifiersEx) {
        if (hovering && !dragging)
            setCursor(cursorFor(dragModeFor(modifiersEx, lastX)));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        // Spell out the hidden range gestures right where they live; note which end is nearer so an
        // Option-drag is unambiguous. Without this, trimming is a covert modifier no one discovers.
        String end = nearestBoundary(e.getX()) == DragMode.RangeStart ? "start" : "end";
        return "<html><b>Timeline</b> — drag to scrub.<br>"
                + "<b>" + ALT_KEY + "</b>-drag an end to <b>trim</b> the movie (nearest here: " + end + ").<br>"
                + "<b>" + MENU_KEY + "</b>-drag to slide the whole range.</html>";
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        hovering = true;
        lastX = e.getX();
        requestFocusInWindow(); // so key press/release events arrive while hovering
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hovering = false;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        wasPlaying = Player.isPlaying();
        if (wasPlaying)
            Commands.pause();
        dragging = true;
        dragMode = dragModeFor(e);
        dragAnchorValue = sliderUI.valueForXPosition(e.getX());
        dragRangeMin = getPlaybackFirstFrame();
        dragRangeMax = getPlaybackLastFrame();
        setCursor(cursorFor(e));
        mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        dragMode = DragMode.Frame;
        setCursor(cursorFor(e));
        if (wasPlaying)
            Commands.play();
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
        return dragModeFor(e.getModifiersEx(), e.getX());
    }

    // Shared by mouse and key events so a modifier press/release refreshes the cursor without moving.
    private DragMode dragModeFor(int modifiersEx, int x) {
        if ((modifiersEx & MENU_SHORTCUT_MASK) != 0)
            return DragMode.Range;
        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0)
            return nearestBoundary(x);
        return DragMode.Frame;
    }

    private Cursor cursorFor(MouseEvent e) {
        return cursorFor(dragModeFor(e));
    }

    private Cursor cursorFor(DragMode dragMode) {
        return switch (dragMode) {
            // Open hand on Cmd-hover ("you can grab and slide the range"); closes to a fist while
            // actually dragging — the same grab affordance as panning the main view.
            case Range -> dragging ? UIGlobals.closedHandCursor : UIGlobals.openHandCursor;
            // A crop cursor reads as "trim" the instant Option is held — a stronger discovery cue
            // than a bare resize arrow for a gesture users don't know exists.
            case RangeStart, RangeEnd -> trimCursor();
            // I-beam for plain scrubbing: it reads as "set the position here".
            case Frame -> Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        };
    }

    private static Cursor trimCursor;

    // A crop-marks glyph drawn to a cursor image (not a font glyph, so it can never fall back to a
    // blank box). White halo under black strokes keeps it visible over any track colour. Falls back
    // to a resize cursor if the platform rejects custom cursors.
    private static Cursor trimCursor() {
        if (trimCursor != null)
            return trimCursor;
        try {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension best = tk.getBestCursorSize(28, 28);
            int s = best.width > 0 ? best.width : 28;
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float a = s * 0.30f, b = s * 0.70f, o = s * 0.16f; // inner square corners + overhang
            Line2D[] marks = {
                    new Line2D.Float(a - o, a, b, a), // top edge, over-hanging left
                    new Line2D.Float(a, a - o, a, b), // left edge, over-hanging up
                    new Line2D.Float(a, b, b + o, b), // bottom edge, over-hanging right
                    new Line2D.Float(b, a, b, b + o), // right edge, over-hanging down
            };
            for (int pass = 0; pass < 2; pass++) {
                g.setColor(pass == 0 ? Color.WHITE : Color.BLACK);
                g.setStroke(new BasicStroke(pass == 0 ? 4f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (Line2D m : marks)
                    g.draw(m);
            }
            g.dispose();
            trimCursor = tk.createCustomCursor(img, new Point(s / 2, s / 2), "trim");
        } catch (Exception e) {
            trimCursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        }
        return trimCursor;
    }

    private DragMode nearestBoundary(int x) {
        int rangeStartX = sliderUI.xPosition(getPlaybackFirstFrame());
        int rangeEndX = sliderUI.xPosition(getPlaybackLastFrame());
        return Math.abs(x - rangeStartX) <= Math.abs(x - rangeEndX) ? DragMode.RangeStart : DragMode.RangeEnd;
    }

    private static int getPlaybackFirstFrame() {
        return ViewState.playbackData().firstFrame();
    }

    private static int getPlaybackLastFrame() {
        return ViewState.playbackData().lastFrame();
    }

    @Override
    public void playbackRangeChanged() {
        repaint();
    }

    @Override
    public void movieStatusChanged() {
        int maximum = Player.isAvailable() ? Player.getMaximumFrameNumber() : 0;
        if (getMaximum() != maximum) {
            setMaximum(maximum);
            repaint();
        }
    }

    private static final class FrameNumberPanel extends JComponent {

        private final Map<?, ?> desktopHints;
        private int frame = -1;
        private int maximum = -1;
        private String text = "";

        FrameNumberPanel(int _value, int _maximum) {
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            desktopHints = hints instanceof Map<?, ?> map ? map : null;
            setFont(UIGlobals.uiFontMonoSmall);
            setForeground(UIGlobals.foreColor);
            setFrame(_value, _maximum);
        }

        void setFrame(int _value, int _maximum) {
            if (frame == _value && maximum == _maximum)
                return;

            boolean maximumChanged = maximum != _maximum;
            frame = _value;
            maximum = _maximum;
            text = (frame + 1) + "/" + (maximum + 1);
            repaint();
            if (maximumChanged)
                revalidate();
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            FontMetrics fm = getFontMetrics(getFont());
            String maximumText = (maximum + 1) + "/" + (maximum + 1);
            return new Dimension(
                    insets.left + fm.stringWidth(maximumText) + insets.right,
                    insets.top + fm.getHeight() + insets.bottom);
        }

        @Override
        protected void paintComponent(Graphics g1) {
            if (text.isEmpty())
                return;

            Graphics2D g = (Graphics2D) g1.create();
            try {
                if (desktopHints != null)
                    g.addRenderingHints(desktopHints);

                g.setColor(getForeground());
                g.setFont(getFont());

                FontMetrics fm = g.getFontMetrics();
                Insets insets = getInsets();
                int x = getWidth() - insets.right - fm.stringWidth(text);
                int y = insets.top + (getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2 + fm.getAscent();
                BasicGraphicsUtils.drawString(this, g, text, x, y);
            } finally {
                g.dispose();
            }
        }

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
            View view;
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer == null) {
                g.setColor(emptyColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else if ((view = layer.getView()).isComplete()) {
                g.setColor(completeColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else {
                int len = view.getMaximumFrameNumber() + 1; // frames are 0...max inclusively
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

            int rangeStartX = xPosition(getPlaybackFirstFrame());
            int rangeEndX = xPosition(getPlaybackLastFrame());
            int left = Math.min(rangeStartX, rangeEndX);
            int right = Math.max(rangeStartX, rangeEndX);
            int width = Math.max(1, right - left + 1);
            g.setColor(rangeColor);
            g.fillRect(left, trackRect.y, width, trackRect.height);
        }

        private void paintRangeMarkers(Graphics2D g) {
            int rangeStartX = xPosition(getPlaybackFirstFrame());
            int rangeEndX = xPosition(getPlaybackLastFrame());
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
