package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 *
 * @author Stephan Pagel
 * */
public class ChartDrawIntervalPane extends JComponent implements ZoomControllerListener, MouseInputListener, LayersListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private Interval<Date> availableInterval = null;
    private Interval<Date> selectedInterval = null;
    private Interval<Date> movieInterval = new Interval<Date>(null, null);

    private boolean mouseOverComponent = false;
    private boolean mouseOverInterval = true;
    private boolean mouseOverLeftGraspPoint = false;
    private boolean mouseOverRightGraspPoint = false;
    private Point mousePressed = null;

    private int leftIntervalBorderPosition = -10;
    private int rightIntervalBorderPosition = -10;

    private final PlotAreaSpace plotAreaSpace;
    private final EVEState eveState;

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(16, 0), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(16, 0), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ChartDrawIntervalPane() {
        initVisualComponents();

        addMouseListener(this);
        addMouseMotionListener(this);
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        Displayer.getLayersModel().addLayersListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        eveState = EVEState.getSingletonInstance();
    }

    private void initVisualComponents() {
        setPreferredSize(new Dimension(getPreferredSize().width, ChartConstants.getIntervalSelectionHeight()));
        setSize(getPreferredSize());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawBackground(g);

        if (availableInterval != null && selectedInterval != null) {
            computeIntervalBorderPositions();

            drawInterval(g);
            drawMovieInterval(g);
            drawLabels(g);
            drawBorders(g);
            drawIntervalGraspPoints(g);
            // drawGraspPointLabels(g);
        }
    }

    private void computeIntervalBorderPositions() {
        final double diffMin = (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime()) / 60000.0;

        long start = selectedInterval.getStart().getTime() - availableInterval.getStart().getTime();
        start = Math.round(start / 60000.0);

        long end = selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime();
        end = Math.round(end / 60000.0);

        final int availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        leftIntervalBorderPosition = (int) ((start / diffMin) * availableIntervalSpace) + ChartConstants.getGraphLeftSpace();
        rightIntervalBorderPosition = (int) ((end / diffMin) * availableIntervalSpace) + ChartConstants.getGraphLeftSpace();
    }

    private void drawBackground(Graphics g) {
        final int availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;

        g.setColor(ChartConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(ChartConstants.getGraphLeftSpace(), 2, availableIntervalSpace, getHeight() - 3);
    }

    private void drawInterval(Graphics g) {
        Graphics2D g2 = ((Graphics2D) g);
        // GradientPaint redtowhite = new GradientPaint(0, getHeight() / 2 -
        // getHeight() / (3 * 2), Color.BLACK, 0, getHeight() / 2
        // + getHeight() / (3 * 2), Color.WHITE);
        // g2.setPaint(redtowhite);
        g2.setColor(Color.black);
        g2.fillRect(leftIntervalBorderPosition, 5, rightIntervalBorderPosition - leftIntervalBorderPosition, 2);
    }

    private void drawMovieInterval(Graphics g) {
        if (availableInterval == null || movieInterval == null || availableInterval.getStart() == null || availableInterval.getEnd() == null || movieInterval.getStart() == null || movieInterval.getEnd() == null) {
            return;
        }

        if (movieInterval.getEnd().getTime() < availableInterval.getStart().getTime() || movieInterval.getStart().getTime() > availableInterval.getEnd().getTime()) {
            return;
        }

        final int availableIntervalWidth = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        final double ratioX = (double) availableIntervalWidth / (double) (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime());

        int min = ChartConstants.getGraphLeftSpace();
        if (availableInterval.containsPointInclusive(movieInterval.getStart())) {
            min += (int) ((movieInterval.getStart().getTime() - availableInterval.getStart().getTime()) * ratioX);
        }

        int max = ChartConstants.getGraphLeftSpace() + availableIntervalWidth;
        if (availableInterval.containsPointInclusive(movieInterval.getEnd())) {
            max = ChartConstants.getGraphLeftSpace() + (int) ((movieInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * ratioX);
        }
        int offset = 0;
        g.setColor(ChartConstants.MOVIE_INTERVAL_COLOR);
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

    private void drawBorders(Graphics g) {
        g.setColor(ChartConstants.BORDER_COLOR);
        // g.drawRect(leftIntervalBorderPosition, getHeight() / 7 * 3,
        // rightIntervalBorderPosition - leftIntervalBorderPosition, getHeight()
        // / 7);

    }

    private void drawIntervalGraspPoints(Graphics g) {
        Graphics2D g2 = ((Graphics2D) g);
        g2.setColor(Color.BLACK);
        // GradientPaint redtowhite = new GradientPaint(0, 0, Color.BLACK, 0,
        // getHeight() - 5, Color.WHITE);
        // g2.setPaint(redtowhite);

        g2.fill(new RoundRectangle2D.Double(leftIntervalBorderPosition - 1, 0, 2, 12, 5, 5));
        g2.fill(new RoundRectangle2D.Double(rightIntervalBorderPosition - 1, 0, 2, 12, 5, 5));

        // g.setColor(ChartConstants.BORDER_COLOR);
        // g2.draw(new RoundRectangle2D.Double(leftIntervalBorderPosition, 0, 2,
        // 12, 5, 5));
        // g2.draw(new RoundRectangle2D.Double(rightIntervalBorderPosition - 2,
        // 0, 2, 12, 5, 5));

    }

    private void drawGraspPointLabels(Graphics g) {
        if (mousePressed != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            final String leftLabelText = ChartConstants.FULL_DATE_TIME_FORMAT.format(selectedInterval.getStart());
            final Rectangle2D leftLabelTextRectangle = g.getFontMetrics().getStringBounds(leftLabelText, g);
            final Rectangle leftRectangle = new Rectangle(leftIntervalBorderPosition - 10 - (int) leftLabelTextRectangle.getWidth(), (getHeight() / 2) - ((int) leftLabelTextRectangle.getHeight() / 2), (int) leftLabelTextRectangle.getWidth(), (int) leftLabelTextRectangle.getHeight());

            if (leftRectangle.x <= 5) {
                leftRectangle.x = leftIntervalBorderPosition + 10;
            }

            final String rightLabelText = ChartConstants.FULL_DATE_TIME_FORMAT.format(selectedInterval.getEnd());
            final Rectangle2D rightLabelTextRectangle = g.getFontMetrics().getStringBounds(rightLabelText, g);
            final Rectangle rightRectangle = new Rectangle(rightIntervalBorderPosition + 10, (getHeight() / 2) - ((int) rightLabelTextRectangle.getHeight() / 2), (int) rightLabelTextRectangle.getWidth(), (int) rightLabelTextRectangle.getHeight());

            if (rightRectangle.x + rightRectangle.width >= getWidth() - 5) {
                rightRectangle.x = rightIntervalBorderPosition - 10 - (int) rightLabelTextRectangle.getWidth();
            }

            if (leftRectangle.intersects(rightRectangle)) {
                leftRectangle.y = (getHeight() / 2) - leftRectangle.y - 2;
                rightRectangle.y = (getHeight() / 2) + 2;
            }

            drawGraspPointLabel(g, leftRectangle, leftLabelText);
            drawGraspPointLabel(g, rightRectangle, rightLabelText);
        }
    }

    private void drawGraspPointLabel(Graphics g, final Rectangle rectangle, final String text) {
        g.setColor(Color.GRAY);
        g.fillRect(rectangle.x - 2, rectangle.y - 1, rectangle.width + 4, rectangle.height);

        g.setColor(ChartConstants.BORDER_COLOR);
        g.drawRect(rectangle.x - 2, rectangle.y - 1, rectangle.width + 4, rectangle.height);

        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.drawString(text, rectangle.x, rectangle.y - 2 + rectangle.height);
    }

    private void drawLabels(Graphics g) {
        if (availableInterval.getStart() == null || availableInterval.getEnd() == null || availableInterval.getStart().getTime() > availableInterval.getEnd().getTime()) {
            return;
        }
        g.setFont(ChartConstants.getFont());

        final int tickTextWidth = (int) g.getFontMetrics().getStringBounds(ChartConstants.FULL_DATE_TIME_FORMAT.format(new Date()), g).getWidth();
        final int availableIntervalWidth = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        final int maxTicks = Math.max(2, (availableIntervalWidth - tickTextWidth * 2) / tickTextWidth);
        final double ratioX = (double) availableIntervalWidth / (double) (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime());

        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(availableInterval.getStart());
        calendar.add(Calendar.YEAR, 3);
        if (availableInterval.containsPointInclusive(calendar.getTime())) {
            drawLabelsYear(g, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        calendar.clear();
        calendar.setTime(availableInterval.getStart());
        calendar.add(Calendar.MONTH, 3);
        if (availableInterval.containsPointInclusive(calendar.getTime())) {
            drawLabelsMonth(g, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        calendar.clear();
        calendar.setTime(availableInterval.getStart());
        calendar.add(Calendar.DAY_OF_YEAR, 3);
        if (availableInterval.containsPointInclusive(calendar.getTime())) {
            drawLabelsDay(g, maxTicks, availableIntervalWidth, ratioX);
            return;
        }

        drawLabelsTime(g, maxTicks, availableIntervalWidth, ratioX);

    }

    private void drawLabelsTime(Graphics g, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final long timeDiff = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
        final double ratioTime = timeDiff / (double) maxTicks;
        int day = -1;
        String tickText;
        for (int i = 0; i < maxTicks; ++i) {
            final Date tickValue = new Date(availableInterval.getStart().getTime() + (long) (i * ratioTime));
            GregorianCalendar tickGreg = new GregorianCalendar();
            tickGreg.setTime(tickValue);
            int currentday = tickGreg.get(GregorianCalendar.DAY_OF_MONTH);
            if (day != currentday) {
                tickText = ChartConstants.FULL_DATE_TIME_FORMAT_NO_SEC.format(tickValue);
                day = currentday;
            } else {
                tickText = ChartConstants.HOUR_TIME_FORMAT_NO_SEC.format(tickValue);
            }

            drawLabel(g, tickText, availableIntervalWidth, tickValue, ratioX);
        }
    }

    private void drawLabelsDay(Graphics g, final int maxTicks, final int availableIntervalWidth, final double ratioX) {

        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(availableInterval.getStart());

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, startDay);

        if (!availableInterval.containsPointInclusive(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);
        startDay = calendar.get(Calendar.DAY_OF_MONTH);

        final long diffMillis = availableInterval.getEnd().getTime() - calendar.getTimeInMillis();
        final int numberOfDays = (int) Math.round(diffMillis / (1000. * 60. * 60. * 24.));
        final int tickCount = Math.min(numberOfDays, maxTicks);
        final double ratioDays = (double) numberOfDays / (double) tickCount;
        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, startDay);
            calendar.add(Calendar.DAY_OF_MONTH, (int) (i * ratioDays));

            final String tickText = ChartConstants.DAY_MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());

            drawLabel(g, tickText, availableIntervalWidth, calendar.getTime(), ratioX);
        }
    }

    private void drawLabelsMonth(Graphics g, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(availableInterval.getStart());

        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.set(startYear, startMonth, 1);

        if (!availableInterval.containsPointInclusive(calendar.getTime())) {
            calendar.add(Calendar.MONTH, 1);
        }

        startYear = calendar.get(Calendar.YEAR);
        startMonth = calendar.get(Calendar.MONTH);

        calendar.clear();
        calendar.setTime(availableInterval.getEnd());

        final int endYear = calendar.get(Calendar.YEAR);
        final int endMonth = calendar.get(Calendar.MONTH);

        final int yearDifference = endYear - startYear;
        final int monthDifference = endMonth - startMonth;
        final int numberOfMonths = monthDifference > 0 ? yearDifference * 12 + monthDifference + 1 : yearDifference * 12 - monthDifference + 1;
        final int tickCount = Math.min(numberOfMonths, maxTicks);
        final double ratioMonth = (double) numberOfMonths / (double) tickCount;

        for (int i = 0; i < maxTicks; ++i) {
            calendar.clear();
            calendar.set(startYear, startMonth, 1);
            calendar.add(Calendar.MONTH, (int) (i * ratioMonth));

            final String tickText = ChartConstants.MONTH_YEAR_TIME_FORMAT.format(calendar.getTime());

            drawLabel(g, tickText, availableIntervalWidth, calendar.getTime(), ratioX);
        }
    }

    private void drawLabelsYear(Graphics g, final int maxTicks, final int availableIntervalWidth, final double ratioX) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(availableInterval.getStart());
        int startYear = calendar.get(Calendar.YEAR);

        calendar.clear();
        calendar.set(startYear, 0, 1);

        if (!availableInterval.containsPointInclusive(calendar.getTime())) {
            startYear++;
        }

        calendar.clear();
        calendar.setTime(availableInterval.getEnd());
        int endYear = calendar.get(Calendar.YEAR);

        final int horizontalTickCount = Math.min(endYear - startYear + 1, maxTicks);
        final int yearDifference = (endYear - startYear) / (horizontalTickCount - 1);

        for (int i = 0; i < horizontalTickCount; ++i) {
            calendar.clear();
            calendar.set(startYear + i * yearDifference, 0, 1);

            final String tickText = ChartConstants.YEAR_ONLY_TIME_FORMAT.format(calendar.getTime());

            drawLabel(g, tickText, availableIntervalWidth, calendar.getTime(), ratioX);
        }
    }

    private void drawLabel(Graphics g, final String tickText, final int availableIntervalWidth, final Date date, final double ratioX) {
        final int textWidth = (int) g.getFontMetrics().getStringBounds(tickText, g).getWidth();
        final int x = ChartConstants.getGraphLeftSpace() + (int) ((date.getTime() - availableInterval.getStart().getTime()) * ratioX);

        if (selectedInterval.containsPointInclusive(date)) {
            g.setColor(ChartConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        } else {
            g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        }

        g.drawLine(x, 2, x, getHeight() - 1);

        g.setColor(ChartConstants.LABEL_TEXT_COLOR);

        if (x + textWidth > ChartConstants.getGraphLeftSpace() + availableIntervalWidth) {
            g.drawString(tickText, x - 2 - textWidth, getHeight() - 3);
        } else {
            g.drawString(tickText, x + 2, getHeight() - 3);
        }
    }

    private void moveSelectedInterval(final Point newMousePosition, boolean forced) {
        final int diffPixel = mousePressed.x > newMousePosition.x ? mousePressed.x - newMousePosition.x : newMousePosition.x - mousePressed.x;
        final double availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
        final double movedUnits = diffPixel / availableIntervalSpace;

        if (mousePressed.x > newMousePosition.x) {
            List<PlotAreaSpace> pasList = new ArrayList<PlotAreaSpace>();
            Map<PlotAreaSpace, Double> minList = new HashMap<PlotAreaSpace, Double>();
            Map<PlotAreaSpace, Double> maxList = new HashMap<PlotAreaSpace, Double>();
            double diffUnits = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            double start = plotAreaSpace.getScaledSelectedMinTime() - movedUnits * diffUnits;
            double end = plotAreaSpace.getScaledSelectedMaxTime() - movedUnits * diffUnits;
            pasList.add(plotAreaSpace);
            if (start < plotAreaSpace.getScaledMinTime()) {
                end += (plotAreaSpace.getScaledMinTime() - start);
                start = plotAreaSpace.getScaledMinTime();
            }
            minList.put(plotAreaSpace, start);
            maxList.put(plotAreaSpace, end);
            for (PlotAreaSpace pas : pasList) {
                if (pas.minMaxTimeIntervalContainsTime(minList.get(pas))) {
                    pas.setScaledSelectedTime(minList.get(pas), maxList.get(pas), forced);
                }
            }
            mousePressed = newMousePosition;
        } else {
            List<PlotAreaSpace> pasList = new ArrayList<PlotAreaSpace>();
            Map<PlotAreaSpace, Double> minList = new HashMap<PlotAreaSpace, Double>();
            Map<PlotAreaSpace, Double> maxList = new HashMap<PlotAreaSpace, Double>();
            double diffUnits = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            double start = plotAreaSpace.getScaledSelectedMinTime() + movedUnits * diffUnits;
            double end = plotAreaSpace.getScaledSelectedMaxTime() + movedUnits * diffUnits;
            if (end > plotAreaSpace.getScaledMaxTime()) {
                start -= (end - plotAreaSpace.getScaledMaxTime());
                end = plotAreaSpace.getScaledMaxTime();
            }
            pasList.add(plotAreaSpace);
            minList.put(plotAreaSpace, start);
            maxList.put(plotAreaSpace, end);
            if (plotAreaSpace.minMaxTimeIntervalContainsTime(maxList.get(plotAreaSpace))) {
                plotAreaSpace.setScaledSelectedTime(minList.get(plotAreaSpace), maxList.get(plotAreaSpace), forced);
            }

            mousePressed = newMousePosition;
        }
    }

    private void resizeSelectedInterval(final Point newMousePosition, boolean forced) {
        int useThisX = newMousePosition.x;
        if (mouseOverLeftGraspPoint) {
            if (newMousePosition.x >= rightIntervalBorderPosition) {
                useThisX = rightIntervalBorderPosition;
            }
            final double availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;

            final double diffUnits = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            final double timestamp = plotAreaSpace.getScaledMinTime() + ((useThisX - ChartConstants.getGraphLeftSpace()) / availableIntervalSpace) * diffUnits;
            plotAreaSpace.setScaledSelectedTime(Math.max(timestamp, plotAreaSpace.getScaledMinTime()), plotAreaSpace.getScaledSelectedMaxTime(), forced);

        } else if (mouseOverRightGraspPoint) {
            if (newMousePosition.x <= leftIntervalBorderPosition) {
                useThisX = leftIntervalBorderPosition;
            }
            final double availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
            final double diffUnits = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            final double timestamp = plotAreaSpace.getScaledMinTime() + (1.0 * (useThisX - ChartConstants.getGraphLeftSpace()) / availableIntervalSpace) * diffUnits;

            plotAreaSpace.setScaledSelectedTime(plotAreaSpace.getScaledSelectedMinTime(), Math.min(timestamp, plotAreaSpace.getScaledMaxTime()), forced);

        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void availableIntervalChanged(final Interval<Date> newInterval) {
        if (newInterval.getStart() == null || newInterval.getEnd() == null) {
            availableInterval = null;
        } else {
            availableInterval = newInterval;
        }
    }

    @Override
    public void selectedIntervalChanged(final Interval<Date> newInterval, boolean keepFullValueSpace) {
        // Log.debug("selected interval" + newInterval);
        // Thread.dumpStack();
        if (newInterval.getStart() == null || newInterval.getEnd() == null) {
            selectedInterval = null;
        } else {
            selectedInterval = newInterval;
        }

        repaint();
    }

    @Override
    public void selectedResolutionChanged(final API_RESOLUTION_AVERAGES newResolution) {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void mouseClicked(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
            if (e.getPoint().x >= ChartConstants.getGraphLeftSpace() && e.getPoint().x <= getWidth() - ChartConstants.getGraphRightSpace()) {
                mousePressed = new Point(leftIntervalBorderPosition + (rightIntervalBorderPosition - leftIntervalBorderPosition) / 2, 0);
                moveSelectedInterval(e.getPoint(), true);
                mousePressed = null;
            }
        } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            jumpSelectedInterval(e.getPoint());
        }

    }

    private void jumpSelectedInterval(Point point) {
        final double availableIntervalSpace = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
        final double position = (point.getX() - ChartConstants.getGraphLeftSpace()) / availableIntervalSpace;
        List<PlotAreaSpace> pasList = new ArrayList<PlotAreaSpace>();
        Map<PlotAreaSpace, Double> minList = new HashMap<PlotAreaSpace, Double>();
        Map<PlotAreaSpace, Double> maxList = new HashMap<PlotAreaSpace, Double>();
        double middlePosition = plotAreaSpace.getScaledSelectedMinTime() + plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime();
        if (position <= middlePosition) {
            // jump to left
            double start = plotAreaSpace.getScaledSelectedMinTime() - (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double end = plotAreaSpace.getScaledSelectedMaxTime() - (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            pasList.add(plotAreaSpace);
            if (start < plotAreaSpace.getScaledMinTime()) {
                end += (plotAreaSpace.getScaledMinTime() - start);
                start = plotAreaSpace.getScaledMinTime();
            }
            minList.put(plotAreaSpace, start);
            maxList.put(plotAreaSpace, end);
        } else {
            // jump to right
            double start = plotAreaSpace.getScaledSelectedMinTime() + (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double end = plotAreaSpace.getScaledSelectedMaxTime() + (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            pasList.add(plotAreaSpace);
            if (end > plotAreaSpace.getScaledMaxTime()) {
                start -= (end - plotAreaSpace.getScaledMaxTime());
                end = plotAreaSpace.getScaledMaxTime();
            }
            minList.put(plotAreaSpace, start);
            maxList.put(plotAreaSpace, end);
        }

        if (plotAreaSpace.minMaxTimeIntervalContainsTime(minList.get(plotAreaSpace))) {
            plotAreaSpace.setScaledSelectedTime(minList.get(plotAreaSpace), maxList.get(plotAreaSpace), true);
        }

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseOverComponent = true;
        if (e.getPoint().x >= ChartConstants.getGraphLeftSpace() && e.getPoint().x <= getWidth() - ChartConstants.getGraphRightSpace()) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        eveState.setMouseTimeIntervalDragging(false);
        if (mousePressed != null) {
            if (mouseOverLeftGraspPoint || mouseOverRightGraspPoint) {
                resizeSelectedInterval(e.getPoint(), true);
            } else if (mouseOverInterval) {
                moveSelectedInterval(e.getPoint(), true);
            }
        }
        mousePressed = null;
        mouseOverComponent = false;
        mouseOverInterval = false;
        mouseOverLeftGraspPoint = false;
        mouseOverRightGraspPoint = false;

        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = e.getPoint();
        if (mouseOverInterval && !mouseOverLeftGraspPoint && !mouseOverRightGraspPoint) {
            setCursor(closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        eveState.setMouseTimeIntervalDragging(false);
        if (mouseOverLeftGraspPoint || mouseOverRightGraspPoint) {
            Log.info(" Mouse released ");
            resizeSelectedInterval(e.getPoint(), true);
        } else if (mouseOverInterval) {
            moveSelectedInterval(e.getPoint(), true);
            setCursor(openHandCursor);
        }
        mousePressed = null;

        repaint();
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Motion Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void mouseDragged(MouseEvent e) {
        eveState.setMouseTimeIntervalDragging(true);
        if (mouseOverInterval) {
            moveSelectedInterval(e.getPoint(), false);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseOverInterval = false;
        mouseOverLeftGraspPoint = false;
        mouseOverRightGraspPoint = false;

        // is mouse cursor above selected interval?
        if (e.getPoint().x >= leftIntervalBorderPosition && e.getPoint().x <= rightIntervalBorderPosition) {
            mouseOverInterval = true;
            setCursor(openHandCursor);
        }

        // reset cursor if it does not point to the interval area
        if (!mouseOverInterval) {
            if (e.getPoint().x >= ChartConstants.getGraphLeftSpace() && e.getPoint().x <= getWidth() - ChartConstants.getGraphRightSpace()) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Layers Listener
    // //////////////////////////////////////////////////////////////////////////////
    @Override
    public void layerAdded(int idx) {

    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(JHVJP2View view) {
        if (view instanceof JHVJPXView) {
            JHVJPXView jpxView = (JHVJPXView) view;
            Date start, end;
            Interval<Date> range = jpxView.getDateRange();

            if (range != null && (start = range.getStart()) != null && (end = range.getEnd()) != null) {
                movieInterval = new Interval<Date>(start, end);
                repaint();
            }
        }
    }

}
