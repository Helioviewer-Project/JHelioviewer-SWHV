package org.helioviewer.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.plugins.eveplugin.EVEPlugin;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.DrawControllerListener;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.model.ChartModel;
import org.helioviewer.plugins.eveplugin.model.ChartModelListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.model.TimeIntervalLockModel;
import org.helioviewer.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * 
 * @author Stephan Pagel
 * */
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener,
        ChartModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;
    private final DrawController drawController;
    private GraphEvent[] events;
    private Map<YAxisElement, Double> yRatios;
    private Date movieTimestamp = null;
    private int movieLinePosition = -1;
    private Point mousePressedPosition = null;
    private Point mouseDragPosition = null;
    private boolean mousePressedOnMovieFrame = false;
    private Rectangle graphArea = new Rectangle();
    private Rectangle plotArea = new Rectangle();
    private double ratioX = 0;
    private int lastKnownWidth = -1;
    private int lastKnownHeight = -1;
    private BufferedImage screenImage = null;
    private final ZoomManager zoomManager;
    private final String identifier;
    private int twoYAxis = 0;
    private final ChartModel chartModel;
    private final PlotAreaSpaceManager plotAreaSpaceManager;
    private final EventModel eventModel;
    private Rectangle leftAxisArea;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public ChartDrawGraphPane(String identifier) {
        chartModel = ChartModel.getSingletonInstance();
        this.identifier = identifier;
        drawController = DrawController.getSingletonInstance();
        initVisualComponents();
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        zoomManager = ZoomManager.getSingletonInstance();
        yRatios = new HashMap<YAxisElement, Double>();
        drawController.addDrawControllerListener(this, identifier);
        chartModel.addChartModelListener(this);
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
        eventModel = EventModel.getSingletonInstance();
    }

    private void initVisualComponents() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        super.paintComponent(g2);
        if (lastKnownWidth != getWidth() || lastKnownHeight != getHeight()) {
            updateGraph();
            lastKnownWidth = getWidth();
            lastKnownHeight = getHeight();
        }
        if (screenImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.drawImage(screenImage, 0, 0, getWidth(), getHeight(), 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
            drawMovieLine(g2);
        }
    }

    private void updateGraph() {
        updateDrawInformation();
        redrawGraph();
    }

    private void redrawGraph() {
        // long start = System.currentTimeMillis();
        int screenfactor = ChartConstants.getScreenfactor();
        int width = screenfactor * getWidth();
        int height = screenfactor * getHeight();
        if (width > 0 && height > 0
                && screenfactor * (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace() + 1) < height
                && screenfactor * (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + 1) < width) {
            screenImage = new BufferedImage(width, height, BufferedImage.OPAQUE);
            final Graphics2D g = screenImage.createGraphics();
            AffineTransform tf = g.getTransform();
            tf.preConcatenate(AffineTransform.getScaleInstance(screenfactor, screenfactor));
            g.setTransform(tf);
            drawBackground(g);

            BufferedImage plotPart = screenImage.getSubimage(screenfactor * ChartConstants.getGraphLeftSpace(), screenfactor
                    * ChartConstants.getGraphTopSpace(), width - screenfactor * ChartConstants.getGraphLeftSpace() - screenfactor
                    * ChartConstants.getGraphRightSpace() - screenfactor * twoYAxis * ChartConstants.getTwoAxisGraphRight(), height
                    - screenfactor * ChartConstants.getGraphTopSpace() - screenfactor * ChartConstants.getGraphBottomSpace());
            Graphics2D gplotPart = plotPart.createGraphics();
            gplotPart.setTransform(tf);
            BufferedImage leftAxisPart = screenImage.getSubimage(0, 0, 2 * ChartConstants.getGraphLeftSpace(), height);
            Graphics2D gleftAxisPart = leftAxisPart.createGraphics();
            gleftAxisPart.setTransform(tf);
            drawData(gplotPart, g, gleftAxisPart);
            drawZoomBox(g);
        }
        this.repaint();
        // Log.info("Run time: " + (System.currentTimeMillis() - start));
    }

    private void drawData(Graphics2D chartg, Graphics2D plotG, Graphics2D leftAxisG) {
        Map<DrawableType, Set<DrawableElement>> drawableElements = drawController.getDrawableElements(identifier);
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
                synchronized (del) {
                    // Log.info("Drawable Elements size : " + del.size());
                    for (DrawableElement de : del) {
                        // Log.info("drawable element" + de);
                        de.draw(chartg, leftAxisG, plotArea, leftAxisArea);
                    }
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
        updateGraphEvents();
    }

    private void drawBackground(final Graphics2D g) {
        g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (mousePressedPosition != null && mouseDragPosition != null) {
            g.setColor(ChartConstants.UNSELECTED_AREA_COLOR);
            g.fillRect(graphArea.x, graphArea.y, graphArea.width, graphArea.height);
        }
    }

    private void drawZoomBox(final Graphics2D g) {
        if (mousePressedPosition == null || mouseDragPosition == null || mousePressedOnMovieFrame) {
            return;
        }

        final int x = mousePressedPosition.x < mouseDragPosition.x ? mousePressedPosition.x : mouseDragPosition.x;
        final int y = mousePressedPosition.y < mouseDragPosition.y ? mousePressedPosition.y : mouseDragPosition.y;
        final int width = Math.abs(mouseDragPosition.x - mousePressedPosition.x);
        final int height = Math.abs(mouseDragPosition.y - mousePressedPosition.y);

        // g.setColor(ChartConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.setColor(Color.BLUE);
        g.drawRect(x, y, width, height);
    }

    private void drawLabels(final Graphics2D g) {
        Set<YAxisElement> yAxisElements = drawController.getYAxisElements(identifier);
        List<YAxisElement> orderedList = orderYAxes(yAxisElements);
        Interval<Date> interval = drawController.getInterval();
        if (!drawController.getIntervalAvailable()) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(ChartConstants.getFont());

        // draw vertical ticks
        int counter = 0;
        synchronized (yAxisElements) {
            for (YAxisElement yAxisElement : orderedList) {
                if (!(yAxisElement.getAvailableRange().max == Double.MIN_VALUE && yAxisElement.getAvailableRange().min == Double.MAX_VALUE)) {

                    drawVerticalLabels(g, yAxisElement, counter == 0 ? 0 : 1);

                    if (counter > 1) {
                        break;
                    }
                    counter++;
                }
            }
        }

        // draw horizontal ticks and labels

        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(
                ChartConstants.FULL_DATE_TIME_FORMAT.format(new Date(interval.getStart().getTime())), g);
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
                if (tickValue.getDay() == previousDate.getDay()) {
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
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }

        // inform when no data is available
        if (!drawController.hasElementsToBeDrawn(identifier)) {
            final String text = ChartConstants.getAbsentText();
            final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
            final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
            final int y = graphArea.y + graphArea.height / 2;

            g.setColor(ChartConstants.LABEL_TEXT_COLOR);
            g.drawString(text, x, y);

        }

        // TODO fit this somewhere in here :-)
        /*
         * final String text = "No data available"; final int textWidth = (int)
         * g.getFontMetrics().getStringBounds(text, g).getWidth(); final int x =
         * graphArea.x + (graphArea.width / 2) - (textWidth / 2); final int y =
         * graphArea.y + graphArea.height / 2;
         * g.setColor(ChartConstants.LABEL_TEXT_COLOR); g.drawString(text, x,
         * y);
         */

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
            g.fillRect(ChartConstants.getGraphLeftSpace() + graphArea.width, ChartConstants.getGraphTopSpace(),
                    ChartConstants.getTwoAxisGraphRight(), graphArea.height);
        } else {
            g.fillRect(ChartConstants.getGraphLeftSpace() + graphArea.width, ChartConstants.getGraphTopSpace(),
                    ChartConstants.getTwoAxisGraphRight() + ChartConstants.getGraphRightSpace(), graphArea.height);
        }
        String verticalLabel = yAxisElement.getLabel();

        if (yAxisElement.isLogScale()) {
            verticalLabel = "log(" + verticalLabel + ")";
        }
        final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
        g.setColor(ChartConstants.LABEL_TEXT_COLOR);
        g.drawString(verticalLabel, (int) (ChartConstants.getGraphLeftSpace() + Math.max(
                (-1 * ChartConstants.getGraphLeftSpace() + 3),
                -((int) verticalLabelBounds.getWidth() / 2 - 3)
                        + leftSide
                        * ((int) verticalLabelBounds.getWidth() / 2 - 3 + graphArea.width - Math.max(verticalLabelBounds.getWidth() / 2,
                                verticalLabelBounds.getWidth() - ChartConstants.getTwoAxisGraphRight())))), (int) verticalLabelBounds
                .getHeight());

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

    private void zoomFromZoomBox() {
        if (mousePressedPosition == null || mouseDragPosition == null) {
            return;
        }

        final int x0 = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, mousePressedPosition.x));
        final int y0 = Math.max(graphArea.y, Math.min(graphArea.y + graphArea.height, mousePressedPosition.y));
        final int x1 = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, mouseDragPosition.x));
        final int y1 = Math.max(graphArea.y, Math.min(graphArea.y + graphArea.height, mouseDragPosition.y));
        if (!(x0 == x1 || y0 == y1)) {
            List<PlotAreaSpace> pass = new ArrayList<PlotAreaSpace>();
            Map<PlotAreaSpace, Double> minTime = new HashMap<PlotAreaSpace, Double>();
            Map<PlotAreaSpace, Double> maxTime = new HashMap<PlotAreaSpace, Double>();
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                pass.add(pas);
                double ratioTime = graphArea.width / (pas.getScaledSelectedMaxTime() - pas.getScaledSelectedMinTime());

                double startTime = pas.getScaledSelectedMinTime() + (Math.min(x0, x1) - graphArea.x) / ratioTime;
                double endTime = pas.getScaledSelectedMinTime() + (Math.max(x0, x1) - graphArea.x) / ratioTime;

                minTime.put(pas, startTime);
                maxTime.put(pas, endTime);
            }
            for (PlotAreaSpace pas : pass) {
                pas.setScaledSelectedTime(minTime.get(pas), maxTime.get(pas), false);
            }

            PlotAreaSpace myPlotAreaSpace = plotAreaSpaceManager.getPlotAreaSpace(identifier);
            double ratioValue = graphArea.height
                    / (myPlotAreaSpace.getScaledSelectedMaxValue() - myPlotAreaSpace.getScaledSelectedMinValue());
            double startValue = (graphArea.y + graphArea.height - Math.max(y0, y1)) / ratioValue
                    + myPlotAreaSpace.getScaledSelectedMinValue();
            double endValue = (graphArea.y + graphArea.height - Math.min(y0, y1)) / ratioValue
                    + myPlotAreaSpace.getScaledSelectedMinValue();

            myPlotAreaSpace.setScaledSelectedValue(startValue, endValue, false);
        }
    }

    private void updateGraphArea() {
        if (drawController.getYAxisElements(identifier).size() >= 2) {
            twoYAxis = 1;
        } else {
            twoYAxis = 0;
        }
        final int graphWidth = getWidth()
                - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + twoYAxis
                        * ChartConstants.getTwoAxisGraphRight());
        final int graphHeight = getHeight() - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace());
        graphArea = new Rectangle(ChartConstants.getGraphLeftSpace(), ChartConstants.getGraphTopSpace(), graphWidth, graphHeight);
        plotArea = new Rectangle(0, 0, graphWidth, graphHeight);
        leftAxisArea = new Rectangle(0, ChartConstants.getGraphTopSpace(), ChartConstants.getGraphLeftSpace(), graphHeight
                - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace()));
        zoomManager.setDisplaySize(plotArea, identifier);
    }

    private void updateRatios() {
        Interval<Date> interval = drawController.getInterval();
        ratioX = !drawController.getIntervalAvailable() ? 0 : (double) graphArea.width
                / (double) (interval.getEnd().getTime() - interval.getStart().getTime());
        yRatios = new HashMap<YAxisElement, Double>();
        for (YAxisElement yAxisElement : drawController.getYAxisElements(identifier)) {
            double logMinValue;
            double logMaxValue;
            if (!yAxisElement.isLogScale()
                    || (yAxisElement.isLogScale() && yAxisElement.getMinValue() > 10e-50 && yAxisElement.getMaxValue() > 10e-50)) {
                if (yAxisElement.isLogScale()) {
                    logMinValue = Math.log10(yAxisElement.getMinValue());
                    logMaxValue = Math.log10(yAxisElement.getMaxValue());
                } else {
                    logMinValue = yAxisElement.getMinValue();
                    logMaxValue = yAxisElement.getMaxValue();
                }
                double ratioY = logMaxValue < logMinValue ? graphArea.height / (logMinValue - logMaxValue) : graphArea.height
                        / (logMaxValue - logMinValue);
                yRatios.put(yAxisElement, ratioY);
            }
        }
    }

    private void updateMovieLineInformation() {
        Interval<Date> interval = drawController.getInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            movieLinePosition = -1;
            return;
        }

        movieLinePosition = (int) ((movieTimestamp.getTime() - interval.getStart().getTime()) * ratioX) + graphArea.x;

        if (movieLinePosition < graphArea.x || movieLinePosition > (graphArea.x + graphArea.width)) {
            movieLinePosition = -1;
        }
    }

    private void updateGraphEvents() {

        // Prepare an ImageIcons to be used with JComponents or drawImage()
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Calendar cal2 = Calendar.getInstance();
        GraphEvent event = new GraphEvent(cal, cal2);
        events = new GraphEvent[1];
        events[0] = event;
    }

    private void setMovieFrameManually(final Point point) {
        Interval<Date> interval = drawController.getInterval();
        if (movieTimestamp == null || !drawController.getIntervalAvailable()) {
            return;
        }

        final int x = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, point.x));
        final long timestamp = ((long) ((x - graphArea.x) / ratioX) + interval.getStart().getTime()) / 1000;

        final LinkedMovieManager linkedMovieManager = LinkedMovieManager.getActiveInstance();
        linkedMovieManager.setCurrentFrame(new ImmutableDateTime(timestamp), new ChangeEvent(), false);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Mouse Input Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void mouseClicked(final MouseEvent e) {
        JHVEvent event = eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y
                - ChartConstants.getGraphTopSpace()));
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
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        boolean repaintFlag = false;

        if (mousePressedPosition != null && mouseDragPosition != null && !mousePressedOnMovieFrame) {
            zoomFromZoomBox();
            repaintFlag = true;
        }

        mousePressedPosition = null;
        mouseDragPosition = null;

        if (repaintFlag) {
            updateGraph();
            repaint();
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        mouseDragPosition = e.getPoint();

        if (mousePressedPosition != null && mousePressedOnMovieFrame) {
            setMovieFrameManually(mouseDragPosition);
        }

        if (mousePressedPosition != null && !mousePressedOnMovieFrame) {
            updateGraph();
            repaint();
        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final Rectangle frame = new Rectangle(movieLinePosition - 1, graphArea.y, 3, graphArea.height);

        if (movieLinePosition >= 0 && drawController.getIntervalAvailable() && frame.contains(e.getPoint())) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else if (eventModel.getEventAtPosition(new Point(e.getPoint().x - ChartConstants.getGraphLeftSpace(), e.getPoint().y
                - ChartConstants.getGraphTopSpace())) != null) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
        updateGraph();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        synchronized (this) {
            updateGraph();
        }
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

    // //////////////////////////////////////////////////////////////////////////////
    // Graph Event
    // //////////////////////////////////////////////////////////////////////////////

    private class GraphEvent {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private ImageIcon icon;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public GraphEvent(Calendar beginDate, Calendar endDate) {
            URL url = EVEPlugin.getResourceUrl("/images/ar_icon.png");
            if (icon == null) {
                icon = new ImageIcon(url);
            }
        }
    }

    @Override
    public void drawRequest() {
        if (!EventQueue.isDispatchThread()) {
            Log.debug("Called outside the Event queue");
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    updateGraph();
                    Log.error("repaint request");
                    Thread.dumpStack();
                    repaint();
                    Log.debug("draw request time: " + (System.currentTimeMillis() - start));
                }
            });
        } else {
            Log.debug("Called in eventQueue");
            long start = System.currentTimeMillis();
            updateGraph();
            repaint();
            Log.debug("draw request time: " + (System.currentTimeMillis() - start));
        }

    }

    @Override
    public void chartRedrawRequested() {
        this.redrawGraph();
    }

    @Override
    public void drawMovieLineRequest(Date time) {
        if (movieTimestamp == null || !movieTimestamp.equals(time)) {
            movieTimestamp = time;

            updateMovieLineInformation();

            if (!TimeIntervalLockModel.getInstance().isLocked()) {
                repaint();
            }
        }
    }
}
