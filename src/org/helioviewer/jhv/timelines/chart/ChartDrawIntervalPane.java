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
import java.awt.geom.RoundRectangle2D;
import java.util.Calendar;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

@SuppressWarnings("serial")
class ChartDrawIntervalPane extends JComponent implements MouseListener, MouseMotionListener, DrawController.Listener {

    private final Calendar calendar = Calendar.getInstance();

    private boolean mouseOverInterval = true;
    private Point mousePressed;

    private int leftIntervalBorderPosition = -10;
    private int rightIntervalBorderPosition = -10;

    ChartDrawIntervalPane() {
        setPreferredSize(new Dimension(-1, DrawConstants.INTERVAL_SELECTION_HEIGHT));

        addMouseListener(this);
        addMouseMotionListener(this);
        DrawController.addDrawListener(this);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(DrawConstants.font);
        drawBackground(g);

        TimeAxis availableAxis = DrawController.availableAxis;
        TimeAxis selectedAxis = DrawController.selectedAxis;

        computeIntervalBorderPositions(availableAxis, selectedAxis);
        drawIntervalBackground(g);
        drawInterval(g);
        drawMovieInterval(g, availableAxis);
        drawLabels(g, availableAxis, selectedAxis);
        drawIntervalGraspPoints(g);
        drawIntervalHBar(g);
    }

    private void drawIntervalBackground(Graphics2D g) {
        g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(leftIntervalBorderPosition - 1, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, getHeight() - 3);
    }

    private void computeIntervalBorderPositions(TimeAxis availableInterval, TimeAxis selectedInterval) {
        long aWidth = availableInterval.end() - availableInterval.start();
        double diffMin = aWidth == 0 ? 1 : aWidth / (double) TimeUtils.MINUTE_IN_MILLIS;
        long start = Math.round((selectedInterval.start() - availableInterval.start()) / (double) TimeUtils.MINUTE_IN_MILLIS);
        long end = Math.round((selectedInterval.end() - availableInterval.start()) / (double) TimeUtils.MINUTE_IN_MILLIS);
        int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;

        leftIntervalBorderPosition = (int) ((start / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
        rightIntervalBorderPosition = (int) ((end / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
    }

    private void drawBackground(Graphics2D g) {
        int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        g.setColor(UIGlobals.TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(DrawConstants.GRAPH_LEFT_SPACE, 2, availableIntervalSpace, getHeight() - 3);
    }

    private void drawInterval(Graphics2D g) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fillRect(leftIntervalBorderPosition, getHeight() - 2, rightIntervalBorderPosition - leftIntervalBorderPosition, 2);
        g.setColor(UIGlobals.TL_BORDER_COLOR);
        g.fillRect(leftIntervalBorderPosition, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, 1);
    }

    private void drawMovieInterval(Graphics2D g, TimeAxis availableInterval) {
        long movieStart = Movie.getStartTime();
        long movieEnd = Movie.getEndTime();

        if (movieEnd < availableInterval.start() || movieStart > availableInterval.end()) {
            return;
        }

        int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        long aWidth = availableInterval.end() - availableInterval.start();
        double ratioX = availableIntervalWidth / (double) (aWidth == 0 ? 1 : aWidth);

        long clampedStart = Math.max(movieStart, availableInterval.start());
        long clampedEnd = Math.min(movieEnd, availableInterval.end());
        int min = DrawConstants.GRAPH_LEFT_SPACE + (int) ((clampedStart - availableInterval.start()) * ratioX);
        int max = DrawConstants.GRAPH_LEFT_SPACE + (int) ((clampedEnd - availableInterval.start()) * ratioX);

        int offset = 7;
        g.setColor(UIGlobals.TL_MOVIE_INTERVAL_COLOR);
        g.drawLine(min, offset, max, offset);
        g.drawLine(min, offset + 2, max, offset + 2);
        g.drawLine(min, offset + 9, max, offset + 9);
        g.drawLine(min, offset + 11, max, offset + 11);

        int width = 1;
        for (int x = min; x <= max; ++x) {
            int mod4 = (x - min) % 4;
            int mod12 = (x - min) % 12;

            if (mod4 == 0) {
                if (mod12 == 0) {
                    g.fillRect(x, offset + 1, width, 10);
                } else {
                    g.fillRect(x, offset + 1, width, 2);
                    g.fillRect(x, offset + 9, width, 2);
                }
            }
        }
    }

    private void drawIntervalGraspPoints(Graphics2D g) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fill(new RoundRectangle2D.Double(leftIntervalBorderPosition - 1, 0, 2, getHeight(), 5, 5));
        g.fill(new RoundRectangle2D.Double(rightIntervalBorderPosition - 1, 0, 2, getHeight(), 5, 5));
    }

    private void drawIntervalHBar(Graphics2D g) {
        g.setColor(UIGlobals.TL_INTERVAL_BORDER_COLOR);
        g.fill(new RoundRectangle2D.Double(DrawConstants.GRAPH_LEFT_SPACE, 0, leftIntervalBorderPosition - DrawConstants.GRAPH_LEFT_SPACE, 2, 5, 5));
        g.fill(new RoundRectangle2D.Double(rightIntervalBorderPosition, 0, getWidth() - rightIntervalBorderPosition - DrawConstants.GRAPH_RIGHT_SPACE, 2, 5, 5));
    }

    private void drawLabels(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval) {
        String tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT, availableInterval.start());
        int tickTextWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        int maxTicks = Math.max(2, (availableIntervalWidth - tickTextWidth * 2) / tickTextWidth);
        long aWidth = availableInterval.end() - availableInterval.start();
        double ratioX = availableIntervalWidth / (double) (aWidth == 0 ? 1 : aWidth);

        long ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 366 * 3;
        if (availableInterval.start() <= ts && ts <= availableInterval.end()) {
            drawLabelsYear(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }
        ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 31 * 3;
        if (availableInterval.start() <= ts && ts <= availableInterval.end()) {
            drawLabelsMonth(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }
        ts = availableInterval.start() + TimeUtils.DAY_IN_MILLIS * 3;
        if (availableInterval.start() <= ts && ts <= availableInterval.end()) {
            drawLabelsDay(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        drawLabelsTime(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
    }

    private void drawLabelsTime(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        long timeDiff = availableInterval.end() - availableInterval.start();
        double ratioTime = timeDiff / (double) maxTicks;
        int day = -1;

        for (int i = 0; i < maxTicks; ++i) {
            long tickValue = availableInterval.start() + (long) (i * ratioTime);
            calendar.setTimeInMillis(tickValue);
            int currentday = calendar.get(Calendar.DAY_OF_MONTH);

            String tickText;
            if (day == currentday) {
                tickText = TimeUtils.format(DrawConstants.HOUR_TIME_FORMAT_NO_SEC, tickValue);
            } else {
                tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT_NO_SEC, tickValue);
                day = currentday;
            }
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, tickValue, ratioX);
        }
    }

    private void drawLabelsDay(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
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
        for (int i = 0; i < tickCount; ++i) {
            calendar.set(startYear, startMonth, startDay, 0, 0, 0);
            calendar.add(Calendar.DAY_OF_MONTH, (int) (i * ratioDays));
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.DAY_MONTH_YEAR_TIME_FORMAT, time);
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, time, ratioX);
        }
    }

    private void drawLabelsMonth(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
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

        for (int i = 0; i < tickCount; ++i) {
            calendar.set(startYear, startMonth, 1, 0, 0, 0);
            calendar.add(Calendar.MONTH, (int) (i * ratioMonth));
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.MONTH_YEAR_TIME_FORMAT, time);
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, time, ratioX);
        }
    }

    private void drawLabelsYear(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        calendar.setTimeInMillis(availableInterval.start());

        int startYear = calendar.get(Calendar.YEAR);

        calendar.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
        long ts = calendar.getTimeInMillis();
        if (!(availableInterval.start() <= ts && ts <= availableInterval.end())) {
            startYear++;
        }

        calendar.setTimeInMillis(availableInterval.end());
        int endYear = calendar.get(Calendar.YEAR);

        int hticks = Math.min(Math.max(endYear - startYear + 1, 2), maxTicks);
        int yearDifference = (endYear - startYear) / (hticks - 1);
        for (int i = 0; i < hticks; ++i) {
            calendar.set(startYear + i * yearDifference, Calendar.JANUARY, 1, 0, 0, 0);
            long time = calendar.getTimeInMillis();

            String tickText = TimeUtils.format(DrawConstants.YEAR_ONLY_TIME_FORMAT, time);
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, time, ratioX);
        }
    }

    private void drawLabel(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, String tickText, int availableIntervalWidth, long date, double ratioX) {
        int textWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        int x = DrawConstants.GRAPH_LEFT_SPACE + (int) ((date - availableInterval.start()) * ratioX);
        if (selectedInterval.start() <= date && date <= selectedInterval.end()) {
            g.setColor(UIGlobals.TL_AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        } else {
            g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        }
        g.drawLine(x, 2, x, getHeight() - 1);
        g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
        if (x + textWidth > DrawConstants.GRAPH_LEFT_SPACE + availableIntervalWidth) {
            if ((x - 2) < DrawConstants.GRAPH_LEFT_SPACE + availableIntervalWidth) {
                g.drawString(tickText, x - 2 - textWidth, getHeight() - 5);
            }
        } else {
            g.drawString(tickText, x + 2, getHeight() - 5);
        }
    }

    private void moveSelectedInterval(Point newMousePosition) {
        if (mousePressed != null) {
            DrawController.moveXAvailableBased(mousePressed.x, newMousePosition.x);
            mousePressed = newMousePosition;
        }
    }

    // Mouse Listener

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
                mousePressed = new Point(leftIntervalBorderPosition + (rightIntervalBorderPosition - leftIntervalBorderPosition) / 2, 0);
                moveSelectedInterval(p);
                mousePressed = null;
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            jumpSelectedInterval(p);
        }
    }

    private void jumpSelectedInterval(Point point) {
        double intervalWidthPixel = (1. * rightIntervalBorderPosition - leftIntervalBorderPosition);
        double middle = leftIntervalBorderPosition + 0.5 * intervalWidthPixel;
        DrawController.moveXAvailableBased(point.x, (int) middle);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        Point p = e.getPoint();
        if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        // repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = e.getPoint();
        if (mouseOverInterval) {
            setCursor(UIGlobals.closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (mouseOverInterval) {
            moveSelectedInterval(p);
            setCursor(UIGlobals.openHandCursor);
        }
        mousePressed = null;
        // repaint();
    }

    // Mouse Motion Listener

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseOverInterval) {
            moveSelectedInterval(e.getPoint());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();

        mouseOverInterval = false;
        // is mouse cursor above selected interval?
        if (p.x >= leftIntervalBorderPosition && p.x <= rightIntervalBorderPosition) {
            mouseOverInterval = true;
            setCursor(UIGlobals.openHandCursor);
        }
        // reset cursor if it does not point to the interval area
        if (!mouseOverInterval) {
            if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    @Override
    public void drawRequest() {
        repaint();
    }

    @Override
    public void drawMovieLineRequest() {
    }

}
