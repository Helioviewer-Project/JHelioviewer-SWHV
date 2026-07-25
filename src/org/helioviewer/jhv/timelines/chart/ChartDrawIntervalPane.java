package org.helioviewer.jhv.timelines.chart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

@SuppressWarnings("serial")
class ChartDrawIntervalPane extends JComponent implements MouseListener, MouseMotionListener, DrawController.Listener {

    private static final Calendar calendar = Calendar.getInstance();

    private boolean draggingInterval;
    private Point mousePressed;

    private record LabelTick(long date, String text) {}

    private record IntervalGeometry(TimeAxis interval, int width, double ratioX, int selectedLeft, int selectedRight) {

        int pixel(long date) {
            return DrawConstants.GRAPH_LEFT_SPACE + (int) ((date - interval.start()) * ratioX);
        }

        int availableRight() {
            return DrawConstants.GRAPH_LEFT_SPACE + width;
        }

        boolean contains(long date) {
            return interval.start() <= date && date <= interval.end();
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
        g.setFont(DrawConstants.font);

        TimeAxis selectedAxis = DrawController.selectedAxis;
        IntervalGeometry geometry = intervalGeometry();

        drawBackground(g, geometry);
        drawIntervalBackground(g, geometry);
        drawInterval(g, geometry);
        drawMovieInterval(g, geometry);
        drawLabels(g, geometry, selectedAxis);
        drawIntervalGraspPoints(g, geometry);
        drawIntervalHBar(g, geometry);
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
        TimeAxis availableInterval = geometry.interval;
        String tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT, availableInterval.start());
        int tickTextWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        int maxTicks = Math.max(2, (geometry.width - tickTextWidth * 2) / tickTextWidth);

        for (LabelTick tick : labelTicks(geometry, maxTicks)) {
            drawLabel(g, selectedInterval, tick.text, tick.date, geometry);
        }
    }

    private static List<LabelTick> labelTicks(IntervalGeometry geometry, int maxTicks) {
        TimeAxis availableInterval = geometry.interval;
        long ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 366 * 3;
        if (geometry.contains(ts)) {
            return yearTicks(availableInterval, maxTicks);
        }

        ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 31 * 3;
        if (geometry.contains(ts)) {
            return monthTicks(availableInterval, maxTicks);
        }

        ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 3;
        return geometry.contains(ts) ? dayTicks(availableInterval, maxTicks) : timeTicks(availableInterval, maxTicks);
    }

    private static List<LabelTick> timeTicks(TimeAxis availableInterval, int maxTicks) {
        List<LabelTick> ticks = new ArrayList<>(maxTicks);
        long timeDiff = availableInterval.end() - availableInterval.start();
        double ratioTime = timeDiff / (double) maxTicks;
        int day = -1;

        for (int i = 0; i < maxTicks; ++i) {
            long tickValue = availableInterval.start() + (long) (i * ratioTime);
            calendar.setTimeInMillis(tickValue);
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

            String tickText;
            if (day == currentDay) {
                tickText = TimeUtils.format(DrawConstants.HOUR_TIME_FORMAT_NO_SEC, tickValue);
            } else {
                tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT_NO_SEC, tickValue);
                day = currentDay;
            }
            ticks.add(new LabelTick(tickValue, tickText));
        }
        return ticks;
    }

    private static List<LabelTick> dayTicks(TimeAxis availableInterval, int maxTicks) {
        calendar.setTimeInMillis(availableInterval.start());

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.set(startYear, startMonth, startDay, 0, 0, 0);
        long ts = calendar.getTimeInMillis();
        if (!(availableInterval.start() <= ts && ts <= availableInterval.end())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);
        startDay = calendar.get(Calendar.DAY_OF_MONTH);

        long diffMillis = availableInterval.end() - calendar.getTimeInMillis();
        int numberOfDays = (int) Math.round(diffMillis / (double) TimeUtils.DAY_IN_MILLIS);
        int tickCount = Math.min(numberOfDays, maxTicks);
        double ratioDays = Math.ceil(numberOfDays / (double) tickCount);
        List<LabelTick> ticks = new ArrayList<>(tickCount);
        for (int i = 0; i < tickCount; ++i) {
            calendar.set(startYear, startMonth, startDay, 0, 0, 0);
            calendar.add(Calendar.DAY_OF_MONTH, (int) (i * ratioDays));
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.DAY_MONTH_YEAR_TIME_FORMAT, time);
            ticks.add(new LabelTick(time, tickText));
        }
        return ticks;
    }

    private static List<LabelTick> monthTicks(TimeAxis availableInterval, int maxTicks) {
        calendar.setTimeInMillis(availableInterval.start());

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);

        calendar.set(startYear, startMonth, 1, 0, 0, 0);
        long ts = calendar.getTimeInMillis();
        if (!(availableInterval.start() <= ts && ts <= availableInterval.end())) {
            calendar.add(Calendar.MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);

        calendar.setTimeInMillis(availableInterval.end());
        int endYear = calendar.get(Calendar.YEAR);
        int endMonth = calendar.get(Calendar.MONTH);

        int numberOfMonths = (endYear - startYear) * 12 + (endMonth - startMonth) + 1;
        int tickCount = Math.min(numberOfMonths, maxTicks);
        double ratioMonth = Math.ceil(numberOfMonths / (double) tickCount);

        List<LabelTick> ticks = new ArrayList<>(tickCount);
        for (int i = 0; i < tickCount; ++i) {
            calendar.set(startYear, startMonth, 1, 0, 0, 0);
            calendar.add(Calendar.MONTH, (int) (i * ratioMonth));
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.MONTH_YEAR_TIME_FORMAT, time);
            ticks.add(new LabelTick(time, tickText));
        }
        return ticks;
    }

    private static List<LabelTick> yearTicks(TimeAxis availableInterval, int maxTicks) {
        calendar.setTimeInMillis(availableInterval.start());

        int startYear = calendar.get(Calendar.YEAR);

        calendar.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
        long ts = calendar.getTimeInMillis();
        if (!(availableInterval.start() <= ts && ts <= availableInterval.end())) {
            startYear++;
        }

        calendar.setTimeInMillis(availableInterval.end());
        int endYear = calendar.get(Calendar.YEAR);

        int hTicks = Math.clamp(endYear - startYear + 1, 2, maxTicks);
        int yearDifference = (endYear - startYear) / (hTicks - 1);
        List<LabelTick> ticks = new ArrayList<>(hTicks);
        for (int i = 0; i < hTicks; ++i) {
            calendar.set(startYear + i * yearDifference, Calendar.JANUARY, 1, 0, 0, 0);
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.YEAR_ONLY_TIME_FORMAT, time);
            ticks.add(new LabelTick(time, tickText));
        }
        return ticks;
    }

    private void drawLabel(Graphics2D g, TimeAxis selectedInterval, String tickText, long date, IntervalGeometry geometry) {
        int textWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        int x = geometry.pixel(date);
        if (selectedInterval.start() <= date && date <= selectedInterval.end()) {
            g.setColor(UIGlobals.TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        } else {
            g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        }
        g.drawLine(x, 2, x, getHeight() - 1);
        g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
        if (x + textWidth > geometry.availableRight()) {
            if ((x - 2) < geometry.availableRight()) {
                g.drawString(tickText, x - 2 - textWidth, getHeight() - 5);
            }
        } else {
            g.drawString(tickText, x + 2, getHeight() - 5);
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
                mousePressed = new Point(geometry.selectedMiddle(), 0);
                moveSelectedInterval(p, geometry);
                mousePressed = null;
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

    @Override
    public void drawMovieLineRequest() {}

}
