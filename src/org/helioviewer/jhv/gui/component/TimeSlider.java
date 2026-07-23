package org.helioviewer.jhv.gui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
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
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;

// Extension of JSlider displaying the caching status on the track.
// This element provides its own look and feel. Therefore, it is independent
// of the global look and feel.
@SuppressWarnings("serial")
public final class TimeSlider extends JSlider implements Interfaces.LazyComponent, MouseListener, MouseMotionListener, MouseWheelListener, Player.Listener, Player.StatusListener, ViewState.PlaybackRangeListener {

    private enum DragMode {
        Frame, Range, RangeStart, RangeEnd
    }

    private static final int RANGE_MARKER_SIZE = 6;
    private static final int MENU_SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Cmd on macOS, Ctrl elsewhere

    private final TimeSliderUI sliderUI;
    private final FrameNumberPanel frameNumberPanel;
    private boolean dirty;
    private boolean wasPlaying;
    private boolean allowSetFrame = true;
    private int dragAnchorValue;
    private int dragRangeMin;
    private int dragRangeMax;
    private DragMode dragMode = DragMode.Frame;

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
        Player.addFrameListener(this);
        Player.addStatusListener(this);
        UITimer.register(this);
        ViewState.addPlaybackRangeListener(this);

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
        wasPlaying = Player.isPlaying();
        if (wasPlaying)
            Commands.pause();
        dragMode = dragModeFor(e);
        dragAnchorValue = sliderUI.valueForXPosition(e.getX());
        dragRangeMin = getPlaybackFirstFrame();
        dragRangeMax = getPlaybackLastFrame();
        setCursor(cursorFor(e));
        mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
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
        if ((e.getModifiersEx() & MENU_SHORTCUT_MASK) != 0)
            return DragMode.Range;
        if (e.isAltDown())
            return nearestBoundary(e.getX()) == DragMode.RangeStart ? DragMode.RangeStart : DragMode.RangeEnd;
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
        private String timeText = ""; // elapsed / total video time, under the frame count

        FrameNumberPanel(int _value, int _maximum) {
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            desktopHints = hints instanceof Map<?, ?> map ? map : null;
            setFont(UIGlobals.uiFontMonoSmall);
            setForeground(UIGlobals.foreColor);
            setFrame(_value, _maximum);
        }

        void setFrame(int _value, int _maximum) {
            boolean maximumChanged = maximum != _maximum;
            frame = _value;
            maximum = _maximum;
            String newText = (frame + 1) + "/" + (maximum + 1);
            String newTimeText = computeTimeText(); // also tracks playback-speed changes at a fixed frame
            if (newText.equals(text) && newTimeText.equals(timeText) && !maximumChanged)
                return;

            text = newText;
            timeText = newTimeText;
            repaint();
            if (maximumChanged)
                revalidate();
        }

        // Video position as elapsed / total real-time, consistent with the frame count above it.
        private String computeTimeText() {
            if (maximum < 1)
                return "";
            double total = ViewState.estimateVideoSeconds(maximum + 1, Player.getEndTime() - Player.getStartTime());
            double elapsed = total * (frame + 1) / (maximum + 1);
            return TimeUtils.formatDurationSig(Math.round(elapsed * 1000)) + " / " + TimeUtils.formatDurationSig(Math.round(total * 1000));
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            FontMetrics fm = getFontMetrics(getFont());
            String maximumText = (maximum + 1) + "/" + (maximum + 1);
            int width = Math.max(fm.stringWidth(maximumText), fm.stringWidth(timeText));
            return new Dimension(
                    insets.left + width + insets.right,
                    insets.top + 2 * fm.getHeight() + insets.bottom); // frame count + video time
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
                int lineHeight = fm.getHeight();
                int blockHeight = timeText.isEmpty() ? lineHeight : 2 * lineHeight;
                int top = insets.top + (getHeight() - insets.top - insets.bottom - blockHeight) / 2;

                int xFrame = getWidth() - insets.right - fm.stringWidth(text);
                BasicGraphicsUtils.drawString(this, g, text, xFrame, top + fm.getAscent());
                if (!timeText.isEmpty()) {
                    int xTime = getWidth() - insets.right - fm.stringWidth(timeText);
                    BasicGraphicsUtils.drawString(this, g, timeText, xTime, top + lineHeight + fm.getAscent());
                }
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
