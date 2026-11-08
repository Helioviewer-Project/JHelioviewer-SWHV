package org.helioviewer.jhv.timelines.chart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.TimeTicks;

@SuppressWarnings("serial")
class ChartDrawIntervalPane extends JComponent implements MouseListener, MouseMotionListener, DrawController.Listener {

    private boolean draggingInterval;
    private Point mousePressed;

    private record IntervalGeometry(TimeAxis interval, int width, double ratioX, int selectedLeft, int selectedRight) {

        int pixel(long date) {
            return DrawConstants.GRAPH_LEFT_SPACE + (int) ((date - interval.start()) * ratioX);
        }

        int availableRight() {
            return DrawConstants.GRAPH_LEFT_SPACE + width;
        }

        boolean containsSelected(Point p) {
            return p.x >= selectedLeft && p.x <= selectedRight;
        }

        int selectedMiddle() {
            return selectedLeft + (selectedRight - selectedLeft) / 2;
        }

        long timeAt(int x) {
            return (long) (interval.start() + (x - DrawConstants.GRAPH_LEFT_SPACE) / ratioX);
        }

    }

    ChartDrawIntervalPane() {
        setPreferredSize(new Dimension(-1, DrawConstants.INTERVAL_SELECTION_HEIGHT));

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        DrawController.addDrawListener(this);
    }

    @Override
    public void removeNotify() {
        DrawController.removeDrawListener(this);
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(TimelineLabelPainter.font);

        TimeAxis selectedAxis = DrawController.selectedAxis;
        IntervalGeometry geometry = intervalGeometry();

        drawBackground(g, geometry);
        drawIntervalBackground(g, geometry);
        drawInterval(g, geometry);
        drawMovieInterval(g, geometry);
        drawIntervalGraspPoints(g, geometry);
        drawIntervalHBar(g, geometry);
        drawLabels(g, geometry, selectedAxis);
    }

    private void drawIntervalBackground(Graphics2D g, IntervalGeometry geometry) {
        g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(geometry.selectedLeft - 1, 0, geometry.selectedRight - geometry.selectedLeft, getHeight() - 3);
    }

    private void drawBackground(Graphics2D g, IntervalGeometry geometry) {
        g.setColor(UIGlobals.TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(DrawConstants.GRAPH_LEFT_SPACE, 2, geometry.width, getHeight() - 3);
    }

    private void drawInterval(Graphics2D g, IntervalGeometry geometry) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fillRect(geometry.selectedLeft, getHeight() - 2, geometry.selectedRight - geometry.selectedLeft, 2);
        g.setColor(UIGlobals.TL_BORDER_COLOR);
        g.fillRect(geometry.selectedLeft, 0, geometry.selectedRight - geometry.selectedLeft, 1);
    }

    private void drawMovieInterval(Graphics2D g, IntervalGeometry geometry) {
        long movieStart = Player.getStartTime();
        long movieEnd = Player.getEndTime();

        if (movieEnd < geometry.interval.start() || movieStart > geometry.interval.end()) {
            return;
        }

        long clampedStart = Math.max(movieStart, geometry.interval.start());
        long clampedEnd = Math.min(movieEnd, geometry.interval.end());
        int min = geometry.pixel(clampedStart);
        int max = geometry.pixel(clampedEnd);

        int offset = 7;
        g.setColor(UIGlobals.TL_MOVIE_INTERVAL_COLOR);
        g.drawLine(min, offset, max, offset);
        g.drawLine(min, offset + 2, max, offset + 2);
        g.drawLine(min, offset + 9, max, offset + 9);
        g.drawLine(min, offset + 11, max, offset + 11);

        for (int x = min, tick = 0; x <= max; x += 4, tick++) {
            if (tick % 3 == 0) {
                g.fillRect(x, offset + 1, 1, 10);
            } else {
                g.fillRect(x, offset + 1, 1, 2);
                g.fillRect(x, offset + 9, 1, 2);
            }
        }
    }

    private void drawIntervalGraspPoints(Graphics2D g, IntervalGeometry geometry) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fillRect(geometry.selectedLeft - 1, 0, 2, getHeight());
        g.fillRect(geometry.selectedRight - 1, 0, 2, getHeight());
    }

    private void drawIntervalHBar(Graphics2D g, IntervalGeometry geometry) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fillRect(DrawConstants.GRAPH_LEFT_SPACE, 0, geometry.selectedLeft - DrawConstants.GRAPH_LEFT_SPACE, 2);
        g.fillRect(geometry.selectedRight, 0, getWidth() - geometry.selectedRight - DrawConstants.GRAPH_RIGHT_SPACE, 2);
    }

    private void drawLabels(Graphics2D g, IntervalGeometry geometry, TimeAxis selectedInterval) {
        TimeAxis available = geometry.interval;
        FontMetrics fontMetrics = g.getFontMetrics();
        TimeTicks.Step step = TimeTicks.step(fontMetrics, available.start(), available.end(), geometry.width);

        long previousDay = Long.MIN_VALUE;
        for (long tick = step.first(available.start()); tick <= available.end(); tick = step.next(tick)) {
            if (tick < available.start())
                continue;
            int x = geometry.pixel(tick);
            boolean selected = selectedInterval.start() <= tick && tick <= selectedInterval.end();
            g.setColor(selected ? UIGlobals.TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR : UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
            g.drawLine(x, 2, x, getHeight() - 1);

            long day = Math.floorDiv(tick, TimeUtils.DAY_IN_MILLIS);
            String text = step.label(tick, day != previousDay);
            if (x + 2 + fontMetrics.stringWidth(text) <= geometry.availableRight()) {
                g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
                g.drawString(text, x + 2, getHeight() - 5);
                previousDay = day;
            }
        }
    }

    private int availableIntervalWidth() {
        return getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
    }

    private IntervalGeometry intervalGeometry() {
        TimeAxis availableInterval = DrawController.availableAxis;
        TimeAxis selectedInterval = DrawController.selectedAxis;
        int pixelWidth = Math.max(1, availableIntervalWidth());
        long intervalWidth = availableInterval.end() - availableInterval.start();
        double ratioX = pixelWidth / (double) (intervalWidth == 0 ? 1 : intervalWidth);
        int selectedLeft = minutePixel(availableInterval, selectedInterval.start(), pixelWidth);
        int selectedRight = minutePixel(availableInterval, selectedInterval.end(), pixelWidth);
        return new IntervalGeometry(availableInterval, pixelWidth, ratioX, selectedLeft, selectedRight);
    }

    private static int minutePixel(TimeAxis interval, long date, int width) {
        long widthMillis = interval.end() - interval.start();
        double minutes = widthMillis == 0 ? 1 : widthMillis / (double) TimeUtils.MINUTE_IN_MILLIS;
        long offsetMinutes = Math.round((date - interval.start()) / (double) TimeUtils.MINUTE_IN_MILLIS);
        return DrawConstants.GRAPH_LEFT_SPACE + (int) ((offsetMinutes / minutes) * width);
    }

    private void moveSelectedInterval(Point newMousePosition, IntervalGeometry geometry) {
        if (mousePressed != null) {
            DrawController.moveSelectedInterval(geometry.timeAt(newMousePosition.x) - geometry.timeAt(mousePressed.x));
            mousePressed = newMousePosition;
        }
    }

    // Mouse Listener

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        IntervalGeometry geometry = intervalGeometry();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= geometry.availableRight()) {
                DrawController.moveSelectedInterval(geometry.timeAt(p.x) - geometry.timeAt(geometry.selectedMiddle()));
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            jumpSelectedInterval(p, geometry);
        }
    }

    private static void jumpSelectedInterval(Point point, IntervalGeometry geometry) {
        DrawController.moveSelectedInterval(geometry.timeAt(geometry.selectedMiddle()) - geometry.timeAt(point.x));
    }

    private void updateCursor(Point p) {
        IntervalGeometry geometry = intervalGeometry();
        if (geometry.containsSelected(p)) {
            setCursor(UIGlobals.openHandCursor);
        } else if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= geometry.availableRight()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = e.getPoint();
        draggingInterval = intervalGeometry().containsSelected(mousePressed);
        if (draggingInterval) {
            setCursor(UIGlobals.closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (draggingInterval) {
            moveSelectedInterval(p, intervalGeometry());
            updateCursor(p);
        }
        draggingInterval = false;
        mousePressed = null;
    }

    // Mouse Motion Listener

    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggingInterval) {
            moveSelectedInterval(e.getPoint(), intervalGeometry());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateCursor(e.getPoint());
    }

    @Override
    public void drawRequest() {
        repaint();
    }

}
