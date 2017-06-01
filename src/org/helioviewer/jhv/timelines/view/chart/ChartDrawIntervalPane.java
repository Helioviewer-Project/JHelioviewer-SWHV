package org.helioviewer.jhv.timelines.view.chart;

import java.awt.Color;
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
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.DrawListener;
import org.helioviewer.jhv.timelines.draw.DrawMovieIntervalListener;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

@SuppressWarnings("serial")
public class ChartDrawIntervalPane extends JComponent implements DrawListener, DrawMovieIntervalListener, MouseListener, MouseMotionListener {

    private long movieStart = System.currentTimeMillis();
    private long movieEnd = System.currentTimeMillis();

    private boolean mouseOverInterval = true;
    private Point mousePressed;

    private int leftIntervalBorderPosition = -10;
    private int rightIntervalBorderPosition = -10;

    public ChartDrawIntervalPane() {
        setPreferredSize(new Dimension(getPreferredSize().width, DrawConstants.INTERVAL_SELECTION_HEIGHT));
        setSize(getPreferredSize());

        addMouseListener(this);
        addMouseMotionListener(this);

        DrawController.addDrawListener(this);
        DrawController.addDrawMovieIntervalListener(this);
    }

    @Override
    protected void paintComponent(Graphics g1) {
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
        drawBorders(g);
        drawIntervalGraspPoints(g);
        drawIntervalHBar(g);
    }

    private void drawIntervalBackground(Graphics2D g) {
        g.setColor(DrawConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(leftIntervalBorderPosition - 1, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, getHeight() - 3);
    }

    private void computeIntervalBorderPositions(TimeAxis availableInterval, TimeAxis selectedInterval) {
        double diffMin = (availableInterval.end - availableInterval.start) / 60000.0;

        long start = selectedInterval.start - availableInterval.start;
        start = Math.round(start / 60000.0);

        long end = selectedInterval.end - availableInterval.start;
        end = Math.round(end / 60000.0);

        int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        leftIntervalBorderPosition = (int) ((start / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
        rightIntervalBorderPosition = (int) ((end / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
    }

    private void drawBackground(Graphics2D g) {
        int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;

        g.setColor(DrawConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(DrawConstants.GRAPH_LEFT_SPACE, 2, availableIntervalSpace, getHeight() - 3);
    }

    private void drawInterval(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(leftIntervalBorderPosition, getHeight() - 2, rightIntervalBorderPosition - leftIntervalBorderPosition, 2);
        g.setColor(DrawConstants.BORDER_COLOR);
        g.fillRect(leftIntervalBorderPosition, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, 1);
    }

    private void drawMovieInterval(Graphics2D g, TimeAxis availableInterval) {
        if (movieEnd < availableInterval.start || movieStart > availableInterval.end) {
            return;
        }

        int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        double ratioX = availableIntervalWidth / (double) (availableInterval.end - availableInterval.start);

        int min = DrawConstants.GRAPH_LEFT_SPACE;
        if (movieStart >= availableInterval.start) {
            min += (int) ((movieStart - availableInterval.start) * ratioX);
        }

        int max = DrawConstants.GRAPH_LEFT_SPACE + availableIntervalWidth;
        if (movieEnd >= availableInterval.start) {
            max = DrawConstants.GRAPH_LEFT_SPACE + (int) ((movieEnd - availableInterval.start) * ratioX);
        }

        int offset = 7;
        g.setColor(DrawConstants.MOVIE_INTERVAL_COLOR);
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

    private static void drawBorders(Graphics2D g) {
        g.setColor(DrawConstants.BORDER_COLOR);
    }

    private void drawIntervalGraspPoints(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(leftIntervalBorderPosition - 1, 0, 2, getHeight(), 5, 5));
        g.fill(new RoundRectangle2D.Double(rightIntervalBorderPosition - 1, 0, 2, getHeight(), 5, 5));
    }

    private void drawIntervalHBar(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(DrawConstants.GRAPH_LEFT_SPACE, 0, leftIntervalBorderPosition - DrawConstants.GRAPH_LEFT_SPACE, 2, 5, 5));
        g.fill(new RoundRectangle2D.Double(rightIntervalBorderPosition, 0, getWidth() - rightIntervalBorderPosition - DrawConstants.GRAPH_RIGHT_SPACE, 2, 5, 5));
    }

    private void drawLabels(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval) {
        if (availableInterval.start > availableInterval.end) {
            return;
        }

        int tickTextWidth = (int) g.getFontMetrics().getStringBounds(DrawConstants.FULL_DATE_TIME_FORMAT.format(new Date()), g).getWidth();
        int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        int maxTicks = Math.max(2, (availableIntervalWidth - tickTextWidth * 2) / tickTextWidth);
        double ratioX = availableIntervalWidth / (double) (availableInterval.end - availableInterval.start);

        long ts = availableInterval.start + TimeUtils.DAY_IN_MILLIS * 366 * 3;
        if (availableInterval.start <= ts && ts <= availableInterval.end) {
            drawLabelsYear(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }
        ts = availableInterval.start + TimeUtils.DAY_IN_MILLIS * 31 * 3;
        if (availableInterval.start <= ts && ts <= availableInterval.end) {
            drawLabelsMonth(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }
        ts = availableInterval.start + TimeUtils.DAY_IN_MILLIS * 3;
        if (availableInterval.start <= ts && ts <= availableInterval.end) {
            drawLabelsDay(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        drawLabelsTime(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
    }

    private void drawLabelsTime(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        long timeDiff = availableInterval.end - availableInterval.start;
        double ratioTime = timeDiff / (double) maxTicks;
        int day = -1;

        GregorianCalendar tickGreg = new GregorianCalendar();
        for (int i = 0; i < maxTicks; ++i) {
            Date tickValue = new Date(availableInterval.start + (long) (i * ratioTime));
            tickGreg.setTime(tickValue);
            int currentday = tickGreg.get(GregorianCalendar.DAY_OF_MONTH);
            String tickText;
            if (day == currentday) {
                tickText = DrawConstants.HOUR_TIME_FORMAT_NO_SEC.format(tickValue);
            } else {
                tickText = DrawConstants.FULL_DATE_TIME_FORMAT_NO_SEC.format(tickValue);
                day = currentday;
            }
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, tickValue.getTime(), ratioX);
        }
    }

    private void drawLabelsDay(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, startDay);
        long ts = calendar.getTime().getTime();
        if (!(availableInterval.start <= ts && ts <= availableInterval.end)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);
        startDay = calendar.get(Calendar.DAY_OF_MONTH);

        long diffMillis = availableInterval.end - calendar.getTimeInMillis();
        int numberOfDays = (int) Math.round(diffMillis / (double) TimeUtils.DAY_IN_MILLIS);
        int tickCount = Math.min(numberOfDays, maxTicks);
        double ratioDays = Math.ceil(numberOfDays / (double) tickCount);
        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, startDay);
            calendar.add(Calendar.DAY_OF_MONTH, (int) (i * ratioDays));

            String tickText = DrawConstants.DAY_MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabelsMonth(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, 1);
        long ts = calendar.getTime().getTime();
        if (!(availableInterval.start <= ts && ts <= availableInterval.end)) {
            calendar.add(Calendar.MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.setTime(new Date(availableInterval.end));

        int endYear = calendar.get(Calendar.YEAR);
        int endMonth = calendar.get(Calendar.MONTH);

        int yearDifference = endYear - startYear;
        int monthDifference = endMonth - startMonth;
        int numberOfMonths = monthDifference > 0 ? yearDifference * 12 + monthDifference + 1 : yearDifference * 12 - monthDifference + 1;
        int tickCount = Math.min(numberOfMonths, maxTicks);
        double ratioMonth = Math.ceil(numberOfMonths / (double) tickCount);

        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, 1);
            calendar.add(Calendar.MONTH, (int) (i * ratioMonth));

            String tickText = DrawConstants.MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabelsYear(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, int maxTicks, int availableIntervalWidth, double ratioX) {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));

        int startYear = calendar.get(Calendar.YEAR);

        calendar.clear();
        calendar.set(startYear, 0, 1);
        long ts = calendar.getTime().getTime();
        if (!(availableInterval.start <= ts && ts <= availableInterval.end)) {
            startYear++;
        }

        calendar.clear();
        calendar.setTime(new Date(availableInterval.end));
        int endYear = calendar.get(Calendar.YEAR);

        int hticks = Math.min(Math.max(endYear - startYear + 1, 2), maxTicks);
        int yearDifference = (endYear - startYear) / (hticks - 1);
        for (int i = 0; i < hticks; ++i) {
            calendar.clear();
            calendar.set(startYear + i * yearDifference, 0, 1);

            String tickText = DrawConstants.YEAR_ONLY_TIME_FORMAT.format(calendar.getTime());
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabel(Graphics2D g, TimeAxis availableInterval, TimeAxis selectedInterval, String tickText, int availableIntervalWidth, long date, double ratioX) {
        int textWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        int x = DrawConstants.GRAPH_LEFT_SPACE + (int) ((date - availableInterval.start) * ratioX);
        if (selectedInterval.start <= date && date <= selectedInterval.end) {
            g.setColor(DrawConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        } else {
            g.setColor(DrawConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        }
        g.drawLine(x, 2, x, getHeight() - 1);
        g.setColor(DrawConstants.LABEL_TEXT_COLOR);
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

    @Override
    public void movieIntervalChanged(long start, long end) {
        movieStart = start;
        movieEnd = end;
        repaint();
    }

}
