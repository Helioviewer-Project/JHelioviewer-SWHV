package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.plugins.eveplugin.draw.DrawController;
import org.helioviewer.plugins.eveplugin.draw.DrawControllerListener;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.draw.TimeIntervalLockModel;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 *
 * @author Stephan Pagel
 * */
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener, MouseWheelListener, WindowFocusListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;
    private final DrawController drawController;
    private Map<YAxisElement, Double> yRatios;
    private Date movieTimestamp = null;
    private int movieLinePosition = -1;
    private Point mousePressedPosition = null;
    private Point mouseDragPosition = null;
    private boolean mousePressedOnMovieFrame = false;
    private Rectangle graphArea = new Rectangle();
    private Rectangle plotArea = new Rectangle();
    private double ratioX = 0;
    private BufferedImage screenImage = null;
    private int twoYAxis = 0;
    private final PlotAreaSpace plotAreaSpace;
    private final EventModel eventModel;
    private Rectangle leftAxisArea;

    private static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(16, 0), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());
    private static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(16, 0), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());
    private boolean reschedule = false;
    private Point mousePosition;
    private boolean mouseOverEvent;
    private int lastWidth;
    private int lastHeight;
    private boolean updateRequestReceived;
    private final Timer timer;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ChartDrawGraphPane() {
        updateRequestReceived = false;
        drawController = DrawController.getSingletonInstance();
        initVisualComponents();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        // addKeyListener(this);
        yRatios = new HashMap<YAxisElement, Double>();
        drawController.addDrawControllerListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        eventModel = EventModel.getSingletonInstance();
        // ImageViewerGui.getMainFrame().addWindowFocusListener(this);
        timer = new Timer("ChartDrawGraphPane redraw timer");
        timer.schedule(new RedrawTimerTask(), 0, (long) (1000.0 / 20));
        // timer.schedule(new RedrawTimerTask(), 0, (long) (3000.0));
        setChartInformation();
    }

    private void initVisualComponents() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        super.paintComponent(g2);
        if (screenImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.drawImage(screenImage, 0, 0, getWidth(), getHeight(), 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
            drawMovieLine(g2);
        }
        if (reschedule && !TimeIntervalLockModel.getInstance().isLocked()) {
            reschedule = false;
            updateGraph();
        }
    }

    private void updateGraph() {
        updateRequestReceived = true;
    }

    private void timerRedrawGraph() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateDrawInformation();
                redrawGraph();
            }
        });
    }

    private void redrawGraph() {
        int screenfactor = ChartConstants.getScreenfactor();
        int width = screenfactor * getWidth();
        int height = screenfactor * getHeight();

        if (width > 0 && height > 0 && screenfactor * (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace() + 1) < height && screenfactor * (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + 1) < width) {
            if (width != lastWidth || height != lastHeight) {
                screenImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
                lastWidth = width;
                lastHeight = height;
            }

            final Graphics2D g = screenImage.createGraphics();
            AffineTransform tf = g.getTransform();
            tf.preConcatenate(AffineTransform.getScaleInstance(screenfactor, screenfactor));
            g.setTransform(tf);
            drawBackground(g);

            BufferedImage plotPart = screenImage.getSubimage(screenfactor * ChartConstants.getGraphLeftSpace(), screenfactor * ChartConstants.getGraphTopSpace(), width - screenfactor * ChartConstants.getGraphLeftSpace() - screenfactor * ChartConstants.getGraphRightSpace() - screenfactor * twoYAxis * ChartConstants.getTwoAxisGraphRight(), height - screenfactor * ChartConstants.getGraphTopSpace() - screenfactor * ChartConstants.getGraphBottomSpace());
            Graphics2D gplotPart = plotPart.createGraphics();
            gplotPart.setTransform(tf);
            BufferedImage leftAxisPart = screenImage.getSubimage(0, 0, 2 * ChartConstants.getGraphLeftSpace(), height);
            Graphics2D gleftAxisPart = leftAxisPart.createGraphics();
            gleftAxisPart.setTransform(tf);
            drawData(gplotPart, g, gleftAxisPart, mousePosition);
            gplotPart.dispose();
            gleftAxisPart.dispose();
            g.dispose();
        }
        this.repaint();
        // Log.info("Run time: " + (System.currentTimeMillis() - start));
    }

    private void drawData(Graphics2D chartg, Graphics2D plotG, Graphics2D leftAxisG, Point mousePosition) {
        Map<DrawableType, Set<DrawableElement>> drawableElements = drawController.getDrawableElements();
        List<DrawableType> drawTypeList = DrawableType.getZOrderedList();
        boolean labelsDrawn = false;
        for (DrawableType dt : drawTypeList) {
            if ((dt != DrawableType.FULL_IMAGE) && !labelsDrawn) {
                drawLabels(plotG);
                labelsDrawn = true;
            }
            Set<DrawableElement> del = drawableElements.get(dt);
            if (del != null) {
                // Log.trace("Drawable element list is not null. Size is " +
                // del.size());
                // Log.info("Drawable Elements size : " + del.size());
                for (DrawableElement de : del) {
                    // Log.info("drawable element" + de);
                    de.draw(chartg, leftAxisG, plotArea, leftAxisArea, mousePosition);
                }

            } else {
                // Log.trace("Drawable element list is null");
            }
        }
    }

    private void updateDrawInformation() {
        updateGraphArea();
        updateRatios();
        updateMovieLineInformation();
    }

    private void drawBackground(final Graphics2D g) {
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawLabels(final Graphics2D g) {
        // Log.debug("Draw Labels");
        // Thread.dumpStack();
        Set<YAxisElement> yAxisElements = drawController.getYAxisElements();
        List<YAxisElement> orderedList = orderYAxes(yAxisElements);
        Interval<Date> interval = drawController.getSelectedInterval();
        if (!drawController.getIntervalAvailable()) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(ChartConstants.getFont());

        // draw vertical ticks
        int counter = 0;

        for (YAxisElement yAxisElement : orderedList) {
            if (!(yAxisElement.getAvailableRange().max == Double.MIN_VALUE && yAxisElement.getAvailableRange().min == Double.MAX_VALUE)) {

                drawVerticalLabels(g, yAxisElement, counter == 0 ? 0 : 1);

                if (counter > 1) {
                    break;
                }
                counter++;
            }
        }

        // draw horizontal ticks and labels

        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(ChartConstants.FULL_DATE_TIME_FORMAT.format(new Date(interval.getStart().getTime())), g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        final int tickTextHeight = (int) tickTextBounds.getHeight();
        final int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        final long tickDifferenceHorizontal = (interval.getEnd().getTime() - interval.getStart().getTime()) / (horizontalTickCount - 1);

        Date previousDate = null;
        for (int i = 0; i < horizontalTickCount; ++i) {
            final Date tickValue = new Date(interval.getStart().getTime() + i * tickDifferenceHorizontal);
            final int x = graphArea.x + (int) (i * tickDifferenceHorizontal * ratioX);
            final String tickText;
            if (previousDate == null) {
                tickText = ChartConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
            } else {
                GregorianCalendar tickGreg = new GregorianCalendar();
                tickGreg.setTime(tickValue);
                GregorianCalendar previousGreg = new GregorianCalendar();
                previousGreg.setTime(previousDate);
                if (tickGreg.get(GregorianCalendar.DAY_OF_MONTH) == previousGreg.get(GregorianCalendar.DAY_OF_MONTH)) {
                    tickText = ChartConstants.HOUR_TIME_FORMAT.format(tickValue);
                } else {
                    tickText = ChartConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
                }
            }

            g.setColor(ChartConstants.TICK_LINE_COLOR);
            g.drawLine(x, graphArea.y, x, graphArea.y + graphArea.height + 3);

            g.setColor(ChartConstants.LABEL_TEXT_COLOR);

            int yl = graphArea.y + graphArea.height + 2 + tickTextHeight;
            for (String line : tickText.split("\n")) {
                tickTextBounds = g.getFontMetrics().getStringBounds(line, g);
                tickTextWidth = (int) tickTextBounds.getWidth();
                int xl = x - (tickTextWidth / 2);
                if (xl + tickTextWidth > getWidth() - ChartConstants.getGraphRightSpace()) {
                    xl = getWidth() - ChartConstants.getGraphRightSpace() - tickTextWidth;
                }
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }

        // inform when no data is available
        if (!drawController.hasElementsToBeDrawn()) {
            final String text = ChartConstants.getAbsentText();
            final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
            final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
            final int y = graphArea.y + graphArea.height / 2;

            g.setColor(ChartConstants.LABEL_TEXT_COLOR);
            g.drawString(text, x, y);

        }
        // Log.debug("Time to draw labels: " + (System.currentTimeMillis() -
        // start) + " total time: " + drawLabelsOperarionTime +
        // " total time over running time : " + timeOverTotalTime);
    }

    private List<YAxisElement> orderYAxes(Set<YAxisElement> yAxisElements) {
        LinkedList<YAxisElement> orderedList = new LinkedList<YAxisElement>();
        for (YAxisElement element : yAxisElements) {
            boolean added = false;
            for (int i = 0; i < orderedList.size(); i++) {
                if (orderedList.get(i).getActivationTime() > element.getActivationTime()) {
                    orderedList.add(i, element);
                    added = true;
                    break;
                }
            }
            if (!added) {
                orderedList.add(orderedList.size(), element);
            }
        }
        return orderedList;
    }

    private void drawVerticalLabels(Graphics g, YAxisElement yAxisElement, int leftSide) {
        // draw rectangle hiding to big radio image
        g.setColor(Color.WHITE);
        if (leftSide == 0) {
            g.fillRect(0, ChartConstants.getGraphTopSpace(), ChartConstants.getGraphLeftSpace(), graphArea.height);
            g.fillRect(ChartConstants.getGraphLeftSpace() + graphArea.width, ChartConstants.getGraphTopSpace(), ChartConstants.getTwoAxisGraphRight(), graphArea.height);
        } else {
            g.fillRect(ChartConstants.getGraphLeftSpace() + graphArea.width, ChartConstants.getGraphTopSpace(), ChartConstants.getTwoAxisGraphRight() + ChartConstants.getGraphRightSpace(), graphArea.height);
        }
        String verticalLabel = yAxisElement.getLabel();

        if (yAxisElement.isLogScale()) {
            verticalLabel = "log(" + verticalLabel + ")";
        }
        final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
        g.setColor(ChartConstants.LABEL_TEXT_COLOR);
        g.drawString(verticalLabel, (int) (ChartConstants.getGraphLeftSpace() + Math.max((-1 * ChartConstants.getGraphLeftSpace() + 3), -((int) verticalLabelBounds.getWidth() / 2 - 3) + leftSide * ((int) verticalLabelBounds.getWidth() / 2 - 3 + graphArea.width - Math.max(verticalLabelBounds.getWidth() / 2, verticalLabelBounds.getWidth() - ChartConstants.getTwoAxisGraphRight())))), (int) verticalLabelBounds.getHeight());

        double minValue = 0.0;
        double maxValue = 0.0;
        if (yAxisElement.isLogScale()) {
            if (yAxisElement.getMinValue() > 10e-50 && yAxisElement.getMaxValue() > 10e-50) {
                minValue = Math.log10(yAxisElement.getMinValue());
                maxValue = Math.log10(yAxisElement.getMaxValue());
            }
        } else {
            minValue = yAxisElement.getMinValue();
            maxValue = yAxisElement.getMaxValue();
        }

        if (maxValue < minValue) {
            final int sizeSteps = graphArea.height / ChartConstants.getMinVerticalTickSpace();
            int verticalTicks = 2;
            if (sizeSteps >= 4) {
                verticalTicks = 5;
            } else if (verticalTicks >= 2) {
                verticalTicks = 3;
            }
            if (verticalTicks == 0) {
                final int y = graphArea.y + graphArea.height;

                g.setColor(ChartConstants.TICK_LINE_COLOR);
                g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
            } else {
                final double tickDifferenceVertical = (minValue - maxValue) / (verticalTicks - 1);

                for (int i = 0; i < verticalTicks; i++) {
                    final double tickValue = maxValue + i * tickDifferenceVertical;
                    String tickText = ChartConstants.DECIMAL_FORMAT.format(tickValue);
                    Double yAxisRatio = yRatios.get(yAxisElement);
                    if (yAxisRatio == null) {
                        continue;
                    }
                    final int y = graphArea.y + graphArea.height - (int) (yAxisRatio * (minValue - tickValue));

                    g.setColor(ChartConstants.TICK_LINE_COLOR);
                    if (leftSide == 0) {
                        g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
                    }

                    final Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
                    final int x = graphArea.x - 6 - (int) bounds.getWidth() + leftSide * (graphArea.width + (int) bounds.getWidth() + 6);
                    g.setColor(ChartConstants.LABEL_TEXT_COLOR);
                    g.drawString(tickText, x, y + (int) (bounds.getHeight() / 2));
                }
            }
        } else {
            final int sizeSteps = graphArea.height / ChartConstants.getMinVerticalTickSpace();
            int verticalTicks = 2;
            if (sizeSteps >= 4) {
                verticalTicks = 5;
            } else if (verticalTicks >= 2) {
                verticalTicks = 3;
            }
            if (verticalTicks == 0) {
                final int y = graphArea.y + graphArea.height;

                g.setColor(ChartConstants.TICK_LINE_COLOR);
                g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
            } else {
                final double tickDifferenceVertical = (maxValue - minValue) / (verticalTicks - 1);

                for (int i = 0; i < verticalTicks; i++) {
                    final double tickValue = minValue + i * tickDifferenceVertical;
                    String tickText = ChartConstants.DECIMAL_FORMAT.format(tickValue);

                    Double yAxisRatio = yRatios.get(yAxisElement);
                    if (yAxisRatio == null) {
                        continue;
                    }
                    final int y = graphArea.y + graphArea.height - (int) (yAxisRatio * (tickValue - minValue));

                    g.setColor(ChartConstants.TICK_LINE_COLOR);
                    if (leftSide == 0) {
                        g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
                    }

                    final Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
                    final int x = graphArea.x - 6 - (int) bounds.getWidth() + leftSide * (graphArea.width + (int) bounds.getWidth() + 6);
                    g.setColor(ChartConstants.LABEL_TEXT_COLOR);
                    g.drawString(tickText, x, y + (int) (bounds.getHeight() / 2));
                    // tickValue += tickDifferenceVertical;

                }
            }

        }
    }

    private void drawMovieLine(final Graphics g) {
        if (movieLinePosition < 0 || !drawController.getIntervalAvailable() || graphArea.height < 0) {
            return;
        }

        g.setColor(ChartConstants.MOVIE_FRAME_COLOR);
        g.drawLine(movieLinePosition, graphArea.y, movieLinePosition, graphArea.y + graphArea.height);
    }

    private void updateGraphArea() {
        graphArea = drawController.getGraphArea();
        plotArea = drawController.getPlotArea();
        leftAxisArea = drawController.getLeftAxisArea();
    }

    private void updateRatios() {
        Interval<Date> interval = drawController.getSelectedInterval();
        ratioX = !drawController.getIntervalAvailable() ? 0 : (double) graphArea.width / (double) (interval.getEnd().getTime() - interval.getStart().getTime());
        yRatios = new HashMap<YAxisElement, Double>();
        for (YAxisElement yAxisElement : drawController.getYAxisElements()) {
            double logMinValue;
            double logMaxValue;
            if (!yAxisElement.isLogScale() || (yAxisElement.isLogScale() && yAxisElement.getMinValue() > 10e-50 && yAxisElement.getMaxValue() > 10e-50)) {
                if (yAxisElement.isLogScale()) {
                    logMinValue = Math.log10(yAxisElement.getMinValue());
                    logMaxValue = Math.log10(yAxisElement.getMaxValue());
                } else {
                    logMinValue = yAxisElement.getMinValue();
                    logMaxValue = yAxisElement.getMaxValue();
                }
                double ratioY = logMaxValue < logMinValue ? graphArea.height / (logMinValue - logMaxValue) : graphArea.height / (logMaxValue - logMinValue);
                yRatios.put(yAxisElement, ratioY);
            }
        }
    }

    private boolean updateMovieLineInformation() {
        int newMovieLine = -1;
        Interval<Date> interval = drawController.getSelectedInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            newMovieLine = -1;
        } else {
            newMovieLine = (int) ((movieTimestamp.getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;
            if (newMovieLine < graphArea.x || newMovieLine > (graphArea.x + graphArea.width)) {
                newMovieLine = -1;
            }
        }
        if (newMovieLine != movieLinePosition) {
            movieLinePosition = newMovieLine;
            return true;
        }
        return false;
    }

    private void setMovieFrameManually(final Point point) {
        Interval<Date> interval = drawController.getSelectedInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            return;
        }

        final int x = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, point.x));
        final long timestamp = ((long) ((x - graphArea.x) / ratioX) + interval.getStart().getTime()) / 1000;

        LinkedMovieManager.getSingletonInstance().setCurrentFrame(new ImmutableDateTime(timestamp), false);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Input Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void mouseClicked(final MouseEvent e) {
        JHVEvent event = eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y - ChartConstants.getGraphTopSpace()));
        if (event != null) {
            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(event);
            dialog.setLocation(e.getLocationOnScreen());
            dialog.validate();
            dialog.pack();
            dialog.setVisible(true);
        } else if (graphArea.contains(e.getPoint())) {
            setMovieFrameManually(e.getPoint());
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    @Override
    public void mouseExited(final MouseEvent e) {
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        final Rectangle movieFrame = new Rectangle(movieLinePosition - 1, graphArea.y, 3, graphArea.height);

        mousePressedOnMovieFrame = movieFrame.contains(e.getPoint());
        // mousePressedPosition = graphArea.contains(e.getPoint()) ?
        // e.getPoint() : null;
        mousePressedPosition = plotArea.contains(e.getPoint()) ? e.getPoint() : null;
        if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height && !(eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y - ChartConstants.getGraphTopSpace())) != null)) {
            setCursor(closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height) {
            setCursor(openHandCursor);
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (mousePressedPosition != null && mouseDragPosition != null && !mousePressedOnMovieFrame) {

            final int mouseX = e.getX();
            final int mouseY = e.getY();
            double distanceX = -1 * (mouseX - mousePressedPosition.x);
            double distanceY = mouseY - mousePressedPosition.y;
            double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double ratioValue = graphArea.height / (plotAreaSpace.getScaledSelectedMaxValue() - plotAreaSpace.getScaledSelectedMinValue());
            double startValue = plotAreaSpace.getScaledSelectedMinValue() + distanceY / ratioValue;
            double startTime = plotAreaSpace.getScaledSelectedMinTime() + distanceX / ratioTime;
            double endValue = startValue + graphArea.height / ratioValue;
            double endTime = startTime + graphArea.width / ratioTime;

            /*
             * if (startTime < myPlotAreaSpace.getScaledMinTime()) { startTime =
             * myPlotAreaSpace.getScaledMinTime(); endTime = startTime +
             * graphArea.width / ratioTime; }
             */
            if (startValue < plotAreaSpace.getScaledMinValue()) {
                startValue = plotAreaSpace.getScaledMinValue();
                endValue = startValue + graphArea.height / ratioValue;
            }
            if (endValue > plotAreaSpace.getScaledMaxValue()) {
                endValue = plotAreaSpace.getScaledMaxValue();
                startValue = endValue - graphArea.height / ratioValue;
            }
            /*
             * if (endTime > myPlotAreaSpace.getScaledMaxTime()) { endTime =
             * myPlotAreaSpace.getScaledMaxTime(); startTime = endTime -
             * graphArea.width / ratioTime; }
             */

            plotAreaSpace.setScaledSelectedTimeAndValue(startTime, endTime, startValue, endValue);
        }

        mousePressedPosition = null;
        mouseDragPosition = null;

    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        mouseDragPosition = e.getPoint();

        if (mousePressedPosition != null && mousePressedOnMovieFrame) {
            setMovieFrameManually(mouseDragPosition);
        }

        if (mousePressedPosition != null && !mousePressedOnMovieFrame) {
            // updateGraph();
            // repaint();
            setCursor(closedHandCursor);
            final int mouseX = e.getX();
            final int mouseY = e.getY();
            double distanceX = -1 * (mouseX - mousePressedPosition.x);
            double distanceY = mouseY - mousePressedPosition.y;
            double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double ratioValue = graphArea.height / (plotAreaSpace.getScaledSelectedMaxValue() - plotAreaSpace.getScaledSelectedMinValue());
            double startValue = plotAreaSpace.getScaledSelectedMinValue() + distanceY / ratioValue;
            double startTime = plotAreaSpace.getScaledSelectedMinTime() + distanceX / ratioTime;
            double endValue = startValue + graphArea.height / ratioValue;
            double endTime = startTime + graphArea.width / ratioTime;

            /*
             * if (startTime < myPlotAreaSpace.getScaledMinTime()) { startTime =
             * myPlotAreaSpace.getScaledMinTime(); endTime = startTime +
             * graphArea.width / ratioTime; }
             */
            if (startValue < plotAreaSpace.getScaledMinValue()) {
                startValue = plotAreaSpace.getScaledMinValue();
                endValue = startValue + graphArea.height / ratioValue;
            }
            if (endValue > plotAreaSpace.getScaledMaxValue()) {
                endValue = plotAreaSpace.getScaledMaxValue();
                startValue = endValue - graphArea.height / ratioValue;
            }
            /*
             * if (endTime > myPlotAreaSpace.getScaledMaxTime()) { endTime =
             * myPlotAreaSpace.getScaledMaxTime(); startTime = endTime -
             * graphArea.width / ratioTime; }
             */

            plotAreaSpace.setScaledSelectedTimeAndValue(startTime, endTime, startValue, endValue);
        }
        mousePressedPosition = e.getPoint();
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        mousePosition = new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y - ChartConstants.getGraphTopSpace());
        final Rectangle frame = new Rectangle(movieLinePosition - 1, graphArea.y, 3, graphArea.height);

        if (movieLinePosition >= 0 && drawController.getIntervalAvailable() && frame.contains(e.getPoint())) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else if (eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y - ChartConstants.getGraphTopSpace())) != null) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            // redrawGraph();
            updateGraph();
            mouseOverEvent = true;
        } else if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height) {
            setCursor(openHandCursor);
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (mouseOverEvent && (eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y - ChartConstants.getGraphTopSpace())) == null)) {
            mouseOverEvent = false;
            updateGraph();
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Component Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
        setChartInformation();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        setChartInformation();
    }

    private void setChartInformation() {
        if (drawController.getYAxisElements().size() >= 2) {
            twoYAxis = 1;
        } else {
            twoYAxis = 0;
        }
        final int graphWidth = getWidth() - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + twoYAxis * ChartConstants.getTwoAxisGraphRight());
        final int graphHeight = getHeight() - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace());
        Rectangle graphArea = new Rectangle(ChartConstants.getGraphLeftSpace(), ChartConstants.getGraphTopSpace(), graphWidth, graphHeight);
        Rectangle plotArea = new Rectangle(0, 0, graphWidth, graphHeight);
        Rectangle leftAxisArea = new Rectangle(0, ChartConstants.getGraphTopSpace(), ChartConstants.getGraphLeftSpace(), graphHeight - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace()));
        drawController.setGraphInformation(graphArea, plotArea, leftAxisArea);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Graph Polyline
    // //////////////////////////////////////////////////////////////////////////////

    public class GraphPolyline {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        public final int numberOfPoints;
        public final int numberOfWarnLevels;
        public final int[] xPoints;
        public final int[] yPoints;
        public final int[] warnLevels;

        public final Color color;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public GraphPolyline(final List<Point> points, final Color color, final List<Integer> warnLevels) {
            numberOfPoints = points.size();
            numberOfWarnLevels = warnLevels.size();
            xPoints = new int[numberOfPoints];
            yPoints = new int[numberOfPoints];
            this.color = color;
            this.warnLevels = new int[numberOfWarnLevels];

            int counter = 0;
            for (final Point point : points) {
                xPoints[counter] = point.x;
                yPoints[counter] = point.y;
                counter++;
            }

            counter = 0;
            for (final Integer warnLevel : warnLevels) {
                this.warnLevels[counter] = warnLevel;
                counter++;
            }
        }
    }

    @Override
    public void drawRequest() {
        updateGraph();
    }

    @Override
    public void drawMovieLineRequest(Date time) {
        if (movieTimestamp == null || !movieTimestamp.equals(time)) {
            movieTimestamp = time;
            if (!TimeIntervalLockModel.getInstance().isLocked()) {
                if (updateMovieLineInformation()) {
                    updateGraph();
                }
            }
        }
        if (time == null) {
            movieTimestamp = null;
            if (updateMovieLineInformation()) {
                updateGraph();
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int scrollValue = e.getWheelRotation();
        double zoomTimeFactor = 10;
        double zoomValueFactor = 1;
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollDistance = e.getScrollAmount();
            final int mouseX = e.getX();
            final int mouseY = e.getY();
            if (mouseX >= graphArea.x && mouseX <= graphArea.x + graphArea.width && mouseY > graphArea.y && mouseY <= graphArea.y + graphArea.height) {
                final double ratioXLeft = (1.0 * (mouseX - graphArea.x) / graphArea.width);
                final double ratioXRight = (1.0 * (graphArea.x + graphArea.width - mouseX) / graphArea.width);
                final double ratioYTop = (1.0 * (mouseY - graphArea.y)) / graphArea.height;
                final double ratioYBottom = (1.0 * (graphArea.y + graphArea.height - mouseY)) / graphArea.height;
                double startTime = plotAreaSpace.getScaledSelectedMinTime();
                double endTime = plotAreaSpace.getScaledSelectedMaxTime();
                double startValue = plotAreaSpace.getScaledSelectedMinValue();
                double endValue = plotAreaSpace.getScaledSelectedMaxValue();

                // zoom in
                if (!e.isAltDown() && !e.isShiftDown()) {
                    double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
                    startTime = plotAreaSpace.getScaledMinTime();
                    endTime = plotAreaSpace.getScaledMaxTime();
                    if (scrollValue < 0) {
                        startTime = plotAreaSpace.getScaledSelectedMinTime() + zoomTimeFactor * scrollDistance * ratioXLeft / ratioTime;
                        endTime = plotAreaSpace.getScaledSelectedMaxTime() - zoomTimeFactor * scrollDistance * ratioXRight / ratioTime;

                    } else {
                        startTime = plotAreaSpace.getScaledSelectedMinTime() - zoomTimeFactor * scrollDistance * ratioXLeft / ratioTime;
                        endTime = plotAreaSpace.getScaledSelectedMaxTime() + zoomTimeFactor * scrollDistance * ratioXRight / ratioTime;
                    }
                    startTime = Math.max(plotAreaSpace.getScaledMinTime(), startTime);
                    endTime = Math.min(plotAreaSpace.getScaledMaxValue(), endTime);
                } else if (e.isShiftDown()) {
                    double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
                    startTime = plotAreaSpace.getScaledMinTime();
                    endTime = plotAreaSpace.getScaledMaxTime();
                    if (scrollValue < 0) {
                        startTime = plotAreaSpace.getScaledSelectedMinTime() - zoomTimeFactor * scrollDistance / ratioTime;
                        endTime = plotAreaSpace.getScaledSelectedMaxTime() - zoomTimeFactor * scrollDistance / ratioTime;

                    } else {
                        startTime = plotAreaSpace.getScaledSelectedMinTime() + zoomTimeFactor * scrollDistance / ratioTime;
                        endTime = plotAreaSpace.getScaledSelectedMaxTime() + zoomTimeFactor * scrollDistance / ratioTime;
                    }
                }

                if ((e.isControlDown() || e.isAltDown()) && !e.isShiftDown()) {
                    double ratioValue = graphArea.height / (plotAreaSpace.getScaledSelectedMaxValue() - plotAreaSpace.getScaledSelectedMinValue());
                    if (scrollValue < 0) {
                        endValue = plotAreaSpace.getScaledSelectedMaxValue() - zoomValueFactor * scrollDistance * ratioYTop / ratioValue;
                        startValue = plotAreaSpace.getScaledSelectedMinValue() + zoomValueFactor * scrollDistance * ratioYBottom / ratioValue;
                    } else {
                        endValue = plotAreaSpace.getScaledSelectedMaxValue() + zoomValueFactor * scrollDistance * ratioYTop / ratioValue;
                        startValue = plotAreaSpace.getScaledSelectedMinValue() - zoomValueFactor * scrollDistance * ratioYBottom / ratioValue;
                    }
                    startValue = Math.max(plotAreaSpace.getScaledMinValue(), startValue);
                    endValue = Math.min(plotAreaSpace.getScaledMaxValue(), endValue);
                }
                if (startValue <= endValue /* && startTime <= endTime */&& startValue >= plotAreaSpace.getScaledMinValue() && startValue <= plotAreaSpace.getScaledMaxValue() && endValue >= plotAreaSpace.getScaledMinValue() && endValue <= plotAreaSpace.getScaledMaxValue() // &&

                // startTime >= myPlotAreaSpace.getScaledMinTime()
                // && endTime <= myPlotAreaSpace.getScaledMaxTime() && startTime
                // <= myPlotAreaSpace.getScaledMaxTime()
                // && endTime >= myPlotAreaSpace.getScaledMinTime()) {
                ) {
                    plotAreaSpace.setScaledSelectedTimeAndValue(startTime, endTime, startValue, endValue);
                }
            }
        }
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        this.requestFocusInWindow();
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        // TODO Auto-generated method stub
    }

    private class RedrawTimerTask extends TimerTask {
        @Override
        public void run() {
            if (updateRequestReceived) {
                // Log.debug("Do drawing");
                updateRequestReceived = false;
                timerRedrawGraph();
            }
        }
    }

}
