package org.helioviewer.jhv.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEState;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.viewmodel.view.View;

// Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class ChartDrawIntervalPane extends JComponent implements TimingListener, MouseInputListener, LayersListener {

    private Interval movieInterval;

    private boolean mouseOverInterval = true;
    // private boolean mouseOverLeftGraspPoint = false;
    // private boolean mouseOverRightGraspPoint = false;
    private Point mousePressed = null;

    private int leftIntervalBorderPosition = -10;
    private int rightIntervalBorderPosition = -10;

    private final DrawController drawController;
    private final EVEState eveState;

    public ChartDrawIntervalPane() {
        initVisualComponents();

        addMouseListener(this);
        addMouseMotionListener(this);
        Layers.addLayersListener(this);
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        eveState = EVEState.getSingletonInstance();
    }

    private void initVisualComponents() {
        setPreferredSize(new Dimension(getPreferredSize().width, DrawConstants.INTERVAL_SELECTION_HEIGHT));
        setSize(getPreferredSize());
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(DrawConstants.font);
        drawBackground(g);

        Interval availableInterval = DrawController.getSingletonInstance().getAvailableInterval();
        Interval selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();

        computeIntervalBorderPositions(availableInterval, selectedInterval);
        drawIntervalBackground(g);
        drawInterval(g);
        drawMovieInterval(g, availableInterval);
        drawLabels(g, availableInterval, selectedInterval);
        drawBorders(g);
        drawIntervalGraspPoints(g);
        drawIntervalHBar(g);
    }

    private void drawIntervalBackground(Graphics2D g) {
        g.setColor(DrawConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(leftIntervalBorderPosition - 1, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, getHeight() - 3);
    }

    private void computeIntervalBorderPositions(Interval availableInterval, Interval selectedInterval) {
        final double diffMin = (availableInterval.end - availableInterval.start) / 60000.0;

        long start = selectedInterval.start - availableInterval.start;
        start = Math.round(start / 60000.0);

        long end = selectedInterval.end - availableInterval.start;
        end = Math.round(end / 60000.0);

        final int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        leftIntervalBorderPosition = (int) ((start / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
        rightIntervalBorderPosition = (int) ((end / diffMin) * availableIntervalSpace) + DrawConstants.GRAPH_LEFT_SPACE;
    }

    private void drawBackground(Graphics2D g) {
        final int availableIntervalSpace = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;

        g.setColor(DrawConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(DrawConstants.GRAPH_LEFT_SPACE, 2, availableIntervalSpace, getHeight() - 3);

    }

    private void drawInterval(Graphics2D g) {
        // final int availableIntervalSpace = getWidth() -
        // (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE +
        // DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        g.setColor(Color.black);
        g.fillRect(leftIntervalBorderPosition, getHeight() - 2, rightIntervalBorderPosition - leftIntervalBorderPosition, 2);
        g.setColor(DrawConstants.BORDER_COLOR);
        g.fillRect(leftIntervalBorderPosition, 0, rightIntervalBorderPosition - leftIntervalBorderPosition, 1);
    }

    private void drawMovieInterval(Graphics2D g, Interval availableInterval) {
        if (availableInterval == null || movieInterval == null) {
            return;
        }

        if (movieInterval.end < availableInterval.start || movieInterval.start > availableInterval.end) {
            return;
        }

        final int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        final double ratioX = availableIntervalWidth / (double) (availableInterval.end - availableInterval.start);

        int min = DrawConstants.GRAPH_LEFT_SPACE;
        if (availableInterval.containsPointInclusive(movieInterval.start)) {
            min += (int) ((movieInterval.start - availableInterval.start) * ratioX);
        }

        int max = DrawConstants.GRAPH_LEFT_SPACE + availableIntervalWidth;
        if (availableInterval.containsPointInclusive(movieInterval.end)) {
            max = DrawConstants.GRAPH_LEFT_SPACE + (int) ((movieInterval.end - availableInterval.start) * ratioX);
        }
        int offset = 7;
        g.setColor(DrawConstants.MOVIE_INTERVAL_COLOR);
        g.drawLine(min, offset, max, offset);
        g.drawLine(min, offset + 2, max, offset + 2);
        g.drawLine(min, offset + 9, max, offset + 9);
        g.drawLine(min, offset + 11, max, offset + 11);
        for (int x = min; x <= max; ++x) {
            final int mod4 = (x - min) % 4;
            final int mod12 = (x - min) % 12;

            if (mod4 == 0) {
                final int width = 1;

                if (mod12 == 0) {
                    g.fillRect(x, offset + 1, width, 10);
                } else {
                    g.fillRect(x, offset + 1, width, 2);
                    g.fillRect(x, offset + 9, width, 2);
                }
            }
        }
    }

    private void drawBorders(Graphics2D g) {
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

    private void drawLabels(Graphics2D g, Interval availableInterval, Interval selectedInterval) {
        if (availableInterval.start > availableInterval.end) {
            return;
        }

        final int tickTextWidth = (int) g.getFontMetrics().getStringBounds(DrawConstants.FULL_DATE_TIME_FORMAT.format(new Date()), g).getWidth();
        final int availableIntervalWidth = getWidth() - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + DrawConstants.RANGE_SELECTION_WIDTH) - 1;
        final int maxTicks = Math.max(2, (availableIntervalWidth - tickTextWidth * 2) / tickTextWidth);
        final double ratioX = availableIntervalWidth / (double) (availableInterval.end - availableInterval.start);

        if (availableInterval.containsPointInclusive(availableInterval.start + TimeUtils.DAY_IN_MILLIS * 366 * 3)) {
            drawLabelsYear(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        if (availableInterval.containsPointInclusive(availableInterval.start + TimeUtils.DAY_IN_MILLIS * 31 * 3)) {
            drawLabelsMonth(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        if (availableInterval.containsPointInclusive(availableInterval.start + TimeUtils.DAY_IN_MILLIS * 3)) {
            drawLabelsDay(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        drawLabelsTime(g, availableInterval, selectedInterval, maxTicks, availableIntervalWidth, ratioX);
    }

    private void drawLabelsTime(Graphics2D g, Interval availableInterval, Interval selectedInterval, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final long timeDiff = availableInterval.end - availableInterval.start;
        final double ratioTime = timeDiff / (double) maxTicks;
        int day = -1;

        GregorianCalendar tickGreg = new GregorianCalendar();
        String tickText;
        for (int i = 0; i < maxTicks; ++i) {
            final Date tickValue = new Date(availableInterval.start + (long) (i * ratioTime));
            tickGreg.setTime(tickValue);
            int currentday = tickGreg.get(GregorianCalendar.DAY_OF_MONTH);
            if (day != currentday) {
                tickText = DrawConstants.FULL_DATE_TIME_FORMAT_NO_SEC.format(tickValue);
                day = currentday;
            } else {
                tickText = DrawConstants.HOUR_TIME_FORMAT_NO_SEC.format(tickValue);
            }
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, tickValue.getTime(), ratioX);
        }
    }

    private void drawLabelsDay(Graphics2D g, Interval availableInterval, Interval selectedInterval, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, startDay);

        if (!availableInterval.containsPointInclusive(calendar.getTime().getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);
        startDay = calendar.get(Calendar.DAY_OF_MONTH);

        final long diffMillis = availableInterval.end - calendar.getTimeInMillis();
        final int numberOfDays = (int) Math.round(diffMillis / (1000. * 60. * 60. * 24.));
        final int tickCount = Math.min(numberOfDays, maxTicks);
        final double ratioDays = Math.ceil((double) numberOfDays / (double) tickCount);
        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, startDay);
            calendar.add(Calendar.DAY_OF_MONTH, (int) (i * ratioDays));

            final String tickText = DrawConstants.DAY_MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());

            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabelsMonth(Graphics2D g, Interval availableInterval, Interval selectedInterval, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, 1);

        if (!availableInterval.containsPointInclusive(calendar.getTime().getTime())) {
            calendar.add(Calendar.MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.setTime(new Date(availableInterval.end));

        final int endYear = calendar.get(Calendar.YEAR);
        final int endMonth = calendar.get(Calendar.MONTH);

        final int yearDifference = endYear - startYear;
        final int monthDifference = endMonth - startMonth;
        final int numberOfMonths = monthDifference > 0 ? yearDifference * 12 + monthDifference + 1 : yearDifference * 12 - monthDifference + 1;
        final int tickCount = Math.min(numberOfMonths, maxTicks);
        final double ratioMonth = Math.ceil((double) numberOfMonths / (double) tickCount);

        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, 1);
            calendar.add(Calendar.MONTH, (int) (i * ratioMonth));

            final String tickText = DrawConstants.MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());
            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabelsYear(Graphics2D g, Interval availableInterval, Interval selectedInterval, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(new Date(availableInterval.start));
        int startYear = calendar.get(Calendar.YEAR);

        calendar.clear();
        calendar.set(startYear, 0, 1);

        if (!availableInterval.containsPointInclusive(calendar.getTime().getTime())) {
            startYear++;
        }

        calendar.clear();
        calendar.setTime(new Date(availableInterval.end));
        int endYear = calendar.get(Calendar.YEAR);

        final int hticks = Math.min(Math.max(endYear - startYear + 1, 2), maxTicks);
        final int yearDifference = (endYear - startYear) / (hticks - 1);

        for (int i = 0; i < hticks; ++i) {
            calendar.clear();
            calendar.set(startYear + i * yearDifference, 0, 1);

            final String tickText = DrawConstants.YEAR_ONLY_TIME_FORMAT.format(calendar.getTime());

            drawLabel(g, availableInterval, selectedInterval, tickText, availableIntervalWidth, calendar.getTime().getTime(), ratioX);
        }
    }

    private void drawLabel(Graphics2D g, Interval availableInterval, Interval selectedInterval, final String tickText, final int availableIntervalWidth, final long date, final double ratioX) {
        final int textWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        final int x = DrawConstants.GRAPH_LEFT_SPACE + (int) ((date - availableInterval.start) * ratioX);
        if (selectedInterval.containsPointInclusive(date)) {
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

    private void moveSelectedInterval(final Point newMousePosition) {
        if (mousePressed != null) {
            final int diffPixel = mousePressed.x > newMousePosition.x ? mousePressed.x - newMousePosition.x : newMousePosition.x - mousePressed.x;
            final int intervalWidthPixel = rightIntervalBorderPosition - leftIntervalBorderPosition;
            if (mousePressed.x > newMousePosition.x) {
                drawController.selectedAxis.move(0, intervalWidthPixel, -diffPixel);
            } else {
                drawController.selectedAxis.move(0, intervalWidthPixel, diffPixel);
            }
            drawController.setSelectedInterval(false, false);
            mousePressed = newMousePosition;
        }
    }

    // Zoom Controller Listener

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        repaint();
    }

    // Mouse Listener

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
                mousePressed = new Point(leftIntervalBorderPosition + (rightIntervalBorderPosition - leftIntervalBorderPosition) / 2, 0);
                moveSelectedInterval(p);
                mousePressed = null;
            }
        } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            jumpSelectedInterval(p);
        }

    }

    private void jumpSelectedInterval(Point point) {
        final double intervalWidthPixel = (1. * rightIntervalBorderPosition - leftIntervalBorderPosition);
        double middle = leftIntervalBorderPosition + 0.5 * intervalWidthPixel;
        double distance = point.getX() - middle;
        Rectangle graphArea = drawController.getGraphArea();
        drawController.selectedAxis.move(graphArea.x, graphArea.width, distance);
        drawController.setSelectedInterval(false, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        Point p = e.getPoint();

        if (p.x >= DrawConstants.GRAPH_LEFT_SPACE && p.x <= getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
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

        eveState.setMouseTimeIntervalDragging(false);
        if (mouseOverInterval) {
            moveSelectedInterval(p);
            setCursor(UIGlobals.openHandCursor);
        }
        mousePressed = null;

        repaint();
    }

    // Mouse Motion Listener

    @Override
    public void mouseDragged(MouseEvent e) {
        eveState.setMouseTimeIntervalDragging(true);
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

    // Layers Listener

    @Override
    public void layerAdded(View view) {
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view != null) {
            movieInterval = new Interval(view.getFirstTime().milli, view.getLastTime().milli);
            repaint();
        }
    }

}
