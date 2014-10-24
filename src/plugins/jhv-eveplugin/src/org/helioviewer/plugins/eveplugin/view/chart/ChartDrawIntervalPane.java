package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
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
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.View;

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
    private boolean mouseOverInterval = false;
    private boolean mouseOverLeftGraspPoint = false;
    private boolean mouseOverRightGraspPoint = false;
    private Point mousePressed = null;

    private int leftIntervalBorderPosition = -10;
    private int rightIntervalBorderPosition = -10;

    private final PlotAreaSpaceManager plotAreaSpaceManager;
    private final EVEState eveState;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ChartDrawIntervalPane() {
        initVisualComponents();

        addMouseListener(this);
        addMouseMotionListener(this);
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
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
            drawGraspPointLabels(g);
        }
    }

    private void computeIntervalBorderPositions() {
        final double diffMin = (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime()) / 60000.0;

        long start = selectedInterval.getStart().getTime() - availableInterval.getStart().getTime();
        start = Math.round(start / 60000.0);

        long end = selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime();
        end = Math.round(end / 60000.0);

        final int availableIntervalSpace = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        leftIntervalBorderPosition = (int) ((start / diffMin) * availableIntervalSpace) + ChartConstants.getGraphLeftSpace();
        rightIntervalBorderPosition = (int) ((end / diffMin) * availableIntervalSpace) + ChartConstants.getGraphLeftSpace();
    }

    private void drawBackground(Graphics g) {
        final int availableIntervalSpace = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;

        g.setColor(ChartConstants.AVAILABLE_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(ChartConstants.getGraphLeftSpace(), 2, availableIntervalSpace, getHeight() - 3);
    }

    private void drawInterval(Graphics g) {
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(leftIntervalBorderPosition, 2, rightIntervalBorderPosition - leftIntervalBorderPosition, getHeight() - 3);
    }

    private void drawMovieInterval(Graphics g) {
        if (availableInterval.getStart() == null || availableInterval.getEnd() == null || movieInterval.getStart() == null
                || movieInterval.getEnd() == null) {
            return;
        }

        if (movieInterval.getEnd().getTime() < availableInterval.getStart().getTime()
                || movieInterval.getStart().getTime() > availableInterval.getEnd().getTime()) {
            return;
        }

        final int availableIntervalWidth = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        final double ratioX = (double) availableIntervalWidth
                / (double) (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime());

        int min = ChartConstants.getGraphLeftSpace();
        if (availableInterval.containsPointInclusive(movieInterval.getStart())) {
            min += (int) ((movieInterval.getStart().getTime() - availableInterval.getStart().getTime()) * ratioX);
        }

        int max = ChartConstants.getGraphLeftSpace() + availableIntervalWidth;
        if (availableInterval.containsPointInclusive(movieInterval.getEnd())) {
            max = ChartConstants.getGraphLeftSpace()
                    + (int) ((movieInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * ratioX);
        }

        g.setColor(ChartConstants.MOVIE_INTERVAL_COLOR);
        g.drawLine(min, 6, max, 6);
        g.drawLine(min, 9, max, 9);
        g.drawLine(min, 17, max, 17);
        g.drawLine(min, 20, max, 20);

        for (int x = min; x <= max; ++x) {
            final int mod4 = (x - min) % 4;
            final int mod12 = (x - min) % 12;

            if (mod4 == 0) {
                final int width = x + 1 > max ? 1 : 2;

                if (mod12 == 0) {
                    g.fillRect(x, 7, width, 13);
                } else {
                    g.fillRect(x, 7, width, 2);
                    g.fillRect(x, 18, width, 2);
                }
            }
        }
    }

    private void drawBorders(Graphics g) {
        g.setColor(ChartConstants.BORDER_COLOR);
        g.drawLine(0, 0, getWidth() - 1 - ChartConstants.getRangeSelectionWidth(), 0);
        g.drawLine(leftIntervalBorderPosition, 2, rightIntervalBorderPosition, 2);
        g.drawLine(leftIntervalBorderPosition, getHeight() - 1, rightIntervalBorderPosition, getHeight() - 1);
        g.drawLine(leftIntervalBorderPosition, 2, leftIntervalBorderPosition, getHeight() - 1);
        g.drawLine(rightIntervalBorderPosition, 2, rightIntervalBorderPosition, getHeight() - 1);
    }

    private void drawIntervalGraspPoints(Graphics g) {
        if (!mouseOverComponent) {
            return;
        }

        Polygon p0 = new Polygon();
        p0.addPoint(leftIntervalBorderPosition, getHeight() / 2 - 5);
        p0.addPoint(leftIntervalBorderPosition - 5, getHeight() / 2);
        p0.addPoint(leftIntervalBorderPosition, getHeight() / 2 + 5);
        p0.addPoint(leftIntervalBorderPosition + 5, getHeight() / 2);

        Polygon p1 = new Polygon();
        p1.addPoint(rightIntervalBorderPosition, getHeight() / 2 - 5);
        p1.addPoint(rightIntervalBorderPosition - 5, getHeight() / 2);
        p1.addPoint(rightIntervalBorderPosition, getHeight() / 2 + 5);
        p1.addPoint(rightIntervalBorderPosition + 5, getHeight() / 2);

        g.setColor(ChartConstants.GRASP_POINT_COLOR);
        g.fillPolygon(p0);
        g.fillPolygon(p1);
    }

    private void drawGraspPointLabels(Graphics g) {
        if (mouseOverInterval && mousePressed != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            final String leftLabelText = ChartConstants.FULL_DATE_TIME_FORMAT.format(selectedInterval.getStart());
            final Rectangle2D leftLabelTextRectangle = g.getFontMetrics().getStringBounds(leftLabelText, g);
            final Rectangle leftRectangle = new Rectangle(leftIntervalBorderPosition - 10 - (int) leftLabelTextRectangle.getWidth(),
                    (getHeight() / 2) - ((int) leftLabelTextRectangle.getHeight() / 2), (int) leftLabelTextRectangle.getWidth(),
                    (int) leftLabelTextRectangle.getHeight());

            if (leftRectangle.x <= 5) {
                leftRectangle.x = leftIntervalBorderPosition + 10;
            }

            final String rightLabelText = ChartConstants.FULL_DATE_TIME_FORMAT.format(selectedInterval.getEnd());
            final Rectangle2D rightLabelTextRectangle = g.getFontMetrics().getStringBounds(rightLabelText, g);
            final Rectangle rightRectangle = new Rectangle(rightIntervalBorderPosition + 10, (getHeight() / 2)
                    - ((int) rightLabelTextRectangle.getHeight() / 2), (int) rightLabelTextRectangle.getWidth(),
                    (int) rightLabelTextRectangle.getHeight());

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
        g.setColor(ChartConstants.GRASP_POINT_COLOR);
        g.fillRect(rectangle.x - 2, rectangle.y - 2, rectangle.width + 4, rectangle.height + 4);

        g.setColor(ChartConstants.BORDER_COLOR);
        g.drawRect(rectangle.x - 2, rectangle.y - 2, rectangle.width + 4, rectangle.height + 4);

        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.drawString(text, rectangle.x, rectangle.y - 2 + rectangle.height);
    }

    private void drawLabels(Graphics g) {
        if (availableInterval.getStart() == null || availableInterval.getEnd() == null
                || availableInterval.getStart().getTime() > availableInterval.getEnd().getTime()) {
            return;
        }

        final int tickTextWidth = (int) g.getFontMetrics().getStringBounds(ChartConstants.FULL_DATE_TIME_FORMAT.format(new Date()), g)
                .getWidth();
        final int availableIntervalWidth = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1;
        final int maxTicks = Math.max(2, (availableIntervalWidth - tickTextWidth * 2) / tickTextWidth);
        final double ratioX = (double) availableIntervalWidth
                / (double) (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime());

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

        for (int i = 0; i < maxTicks; ++i) {
            final Date tickValue = new Date(availableInterval.getStart().getTime() + (long) (i * ratioTime));
            final String tickText = ChartConstants.FULL_DATE_TIME_FORMAT.format(tickValue);

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
        final int numberOfMonths = monthDifference > 0 ? yearDifference * 12 + monthDifference + 1 : yearDifference * 12 - monthDifference
                + 1;
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
        final int diffPixel = mousePressed.x > newMousePosition.x ? mousePressed.x - newMousePosition.x : newMousePosition.x
                - mousePressed.x;
        final double availableIntervalSpace = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
        final double movedUnits = diffPixel / availableIntervalSpace;

        if (mousePressed.x > newMousePosition.x) {
            List<PlotAreaSpace> pasList = new ArrayList<PlotAreaSpace>();
            Map<PlotAreaSpace, Double> minList = new HashMap<PlotAreaSpace, Double>();
            Map<PlotAreaSpace, Double> maxList = new HashMap<PlotAreaSpace, Double>();
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                double diffUnits = pas.getScaledMaxTime() - pas.getScaledMinTime();
                double start = pas.getScaledSelectedMinTime() - movedUnits * diffUnits;
                double end = pas.getScaledSelectedMaxTime() - movedUnits * diffUnits;
                pasList.add(pas);
                if (start < pas.getScaledMinTime()) {
                    end += (pas.getScaledMinTime() - start);
                    start = pas.getScaledMinTime();
                }
                minList.put(pas, start);
                maxList.put(pas, end);
            }
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
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                double diffUnits = pas.getScaledMaxTime() - pas.getScaledMinTime();
                double start = pas.getScaledSelectedMinTime() + movedUnits * diffUnits;
                double end = pas.getScaledSelectedMaxTime() + movedUnits * diffUnits;
                if (end > pas.getScaledMaxTime()) {
                    start -= (end - pas.getScaledMaxTime());
                    end = pas.getScaledMaxTime();
                }
                pasList.add(pas);
                minList.put(pas, start);
                maxList.put(pas, end);
            }
            for (PlotAreaSpace pas : pasList) {
                if (pas.minMaxTimeIntervalContainsTime(maxList.get(pas))) {
                    pas.setScaledSelectedTime(minList.get(pas), maxList.get(pas), forced);
                }
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
            final double availableIntervalSpace = getWidth()
                    - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                final double diffUnits = pas.getScaledMaxTime() - pas.getScaledMinTime();
                final double timestamp = pas.getScaledMinTime() + ((useThisX - ChartConstants.getGraphLeftSpace()) / availableIntervalSpace)
                        * diffUnits;
                pas.setScaledSelectedTime(Math.max(timestamp, pas.getScaledMinTime()), pas.getScaledSelectedMaxTime(), forced);
            }
        } else if (mouseOverRightGraspPoint) {
            if (newMousePosition.x <= leftIntervalBorderPosition) {
                useThisX = leftIntervalBorderPosition;
            }
            final double availableIntervalSpace = getWidth()
                    - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + ChartConstants.getRangeSelectionWidth()) - 1.0;
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                final double diffUnits = pas.getScaledMaxTime() - pas.getScaledMinTime();
                final double timestamp = pas.getScaledMinTime()
                        + (1.0 * (useThisX - ChartConstants.getGraphLeftSpace()) / availableIntervalSpace) * diffUnits;

                pas.setScaledSelectedTime(pas.getScaledSelectedMinTime(), Math.min(timestamp, pas.getScaledMaxTime()), forced);
            }
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
    public void selectedIntervalChanged(final Interval<Date> newInterval) {
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
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseOverComponent = true;
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
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        eveState.setMouseTimeIntervalDragging(false);
        if (mouseOverLeftGraspPoint || mouseOverRightGraspPoint) {
            Log.info(" Mouse released ");
            resizeSelectedInterval(e.getPoint(), true);
        } else if (mouseOverInterval) {
            moveSelectedInterval(e.getPoint(), true);
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
        if (mouseOverLeftGraspPoint || mouseOverRightGraspPoint) {
            resizeSelectedInterval(e.getPoint(), false);
        } else if (mouseOverInterval) {
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
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        // is mouse cursor above of one of the grasp points?
        if (e.getPoint().x >= leftIntervalBorderPosition - 5 && e.getPoint().x <= leftIntervalBorderPosition + 5
                && e.getPoint().y >= (getHeight() / 2 - 5) && e.getPoint().y <= (getHeight() / 2 + 5)) {
            mouseOverLeftGraspPoint = true;
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        }

        if (e.getPoint().x >= rightIntervalBorderPosition - 5 && e.getPoint().x <= rightIntervalBorderPosition + 5
                && e.getPoint().y >= (getHeight() / 2 - 5) && e.getPoint().y <= (getHeight() / 2 + 5)) {
            mouseOverRightGraspPoint = true;
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        }

        // reset cursor if it does not point to the interval area
        if (!mouseOverInterval) {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Layers Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void layerAdded(int idx) {
        movieInterval = LayersModel.getSingletonInstance().getFrameInterval();
        repaint();
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        movieInterval = LayersModel.getSingletonInstance().getFrameInterval();
        repaint();
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

    @Override
    public void viewportGeometryChanged() {
    }

    @Override
    public void timestampChanged(int idx) {
    }

    @Override
    public void subImageDataChanged() {
    }

    @Override
    public void layerDownloaded(int idx) {
    }
}
