package org.helioviewer.jhv.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeIntervalLockModel;
import org.helioviewer.jhv.plugins.eveplugin.draw.ValueSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;

/**
 *
 * @author Stephan Pagel
 */
// Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener, MouseWheelListener {

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

    private boolean reschedule = false;
    private Point mousePosition;
    private boolean mouseOverEvent;
    private int lastWidth;
    private int lastHeight;
    private boolean updateRequestReceived;
    private final Timer timer;
    private final List<DrawableType> zOrderList = new ArrayList<DrawableType>(EnumSet.allOf(DrawableType.class));
    private boolean movieLineRequest = false;
    private boolean forceRedrawGraph = false;

    public ChartDrawGraphPane() {
        updateRequestReceived = false;
        drawController = DrawController.getSingletonInstance();
        initVisualComponents();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        yRatios = new HashMap<YAxisElement, Double>();
        drawController.addDrawControllerListener(this);
        plotAreaSpace = PlotAreaSpace.getSingletonInstance();
        eventModel = EventModel.getSingletonInstance();
        timer = new Timer("ChartDrawGraphPane redraw timer");
        timer.schedule(new RedrawTimerTask(), 0, (long) (1000.0 / 20));
        setChartInformation();
    }

    private void initVisualComponents() {
        setOpaque(true);
        setDoubleBuffered(false);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);

        Graphics2D g = (Graphics2D) g1;
        if (screenImage != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(screenImage, 0, 0, getWidth(), getHeight(), 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
            drawMovieLine(g);
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
        int sx = GLInfo.pixelScale[0], sy = GLInfo.pixelScale[1];
        int width = sx * getWidth();
        int height = sy * getHeight();

        if (width > 0 && height > 0 && sy * (DrawConstants.getGraphTopSpace() + DrawConstants.getGraphBottomSpace() + 1) < height && sx * (DrawConstants.getGraphLeftSpace() + DrawConstants.getGraphRightSpace() + 1) < width && (!movieLineRequest || forceRedrawGraph)) {
            if (width != lastWidth || height != lastHeight) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice device = env.getDefaultScreenDevice();
                GraphicsConfiguration config = device.getDefaultConfiguration();

                screenImage = config.createCompatibleImage(width, height, Transparency.OPAQUE);
                lastWidth = width;
                lastHeight = height;
            }

            final Graphics2D g = screenImage.createGraphics();
            AffineTransform tf = g.getTransform();
            tf.preConcatenate(AffineTransform.getScaleInstance(sx, sy));
            g.setTransform(tf);
            drawBackground(g);
            BufferedImage plotPart = screenImage.getSubimage(sx * DrawConstants.getGraphLeftSpace(), sy * DrawConstants.getGraphTopSpace(), width - sx * (DrawConstants.getGraphLeftSpace() + DrawConstants.getGraphRightSpace() + twoYAxis * DrawConstants.getTwoAxisGraphRight()), height - sy * (DrawConstants.getGraphTopSpace() + DrawConstants.getGraphBottomSpace()));
            Graphics2D gplotPart = plotPart.createGraphics();
            gplotPart.setTransform(tf);
            BufferedImage leftAxisPart = screenImage.getSubimage(0, 0, 2 * DrawConstants.getGraphLeftSpace(), height);
            Graphics2D gleftAxisPart = leftAxisPart.createGraphics();
            gleftAxisPart.setTransform(tf);
            gplotPart.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gplotPart.setFont(DrawConstants.font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(DrawConstants.font);
            gleftAxisPart.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gleftAxisPart.setFont(DrawConstants.font);
            drawData(gplotPart, g, gleftAxisPart, mousePosition);
            gplotPart.dispose();
            gleftAxisPart.dispose();
            g.dispose();
        }
        ExportMovie.EVEImage = screenImage;
        this.repaint();
        movieLineRequest = false;
        forceRedrawGraph = false;
        // Log.info("Run time: " + (System.currentTimeMillis() - start));
    }

    private void drawData(Graphics2D chartg, Graphics2D plotG, Graphics2D leftAxisG, Point mousePosition) {
        Map<DrawableType, Set<DrawableElement>> drawableElements = drawController.getDrawableElements();
        List<DrawableType> drawTypeList = zOrderList;
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
        g.setColor(DrawConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawLabels(final Graphics2D g) {
        // Log.debug("Draw Labels");
        // Thread.dumpStack();
        List<YAxisElement> yAxisElements = drawController.getYAxisElements();
        Interval<Date> interval = drawController.getSelectedInterval();
        if (!drawController.getIntervalAvailable()) {
            return;
        }

        // draw vertical ticks
        int counter = 0;

        for (YAxisElement yAxisElement : yAxisElements) {
            if (!(yAxisElement.getAvailableRange().max == Double.MIN_VALUE && yAxisElement.getAvailableRange().min == Double.MAX_VALUE)) {
                drawVerticalLabels(g, yAxisElement, counter == 0 ? 0 : 1);
                if (counter > 1) {
                    break;
                }
                counter++;
            }
        }

        // draw horizontal ticks and labels

        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(DrawConstants.FULL_DATE_TIME_FORMAT.format(new Date(interval.getStart().getTime())), g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        final int tickTextHeight = (int) tickTextBounds.getHeight();
        final int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        final long tickDifferenceHorizontal = (interval.getEnd().getTime() - interval.getStart().getTime()) / (horizontalTickCount - 1);

        GregorianCalendar tickGreg = new GregorianCalendar();
        GregorianCalendar previousGreg = new GregorianCalendar();

        Date previousDate = null;
        for (int i = 0; i < horizontalTickCount; ++i) {
            final Date tickValue = new Date(interval.getStart().getTime() + i * tickDifferenceHorizontal);
            final int x = graphArea.x + (int) (i * tickDifferenceHorizontal * ratioX);
            final String tickText;
            if (previousDate == null) {
                tickText = DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
            } else {
                tickGreg.setTime(tickValue);
                previousGreg.setTime(previousDate);
                if (tickGreg.get(GregorianCalendar.DAY_OF_MONTH) == previousGreg.get(GregorianCalendar.DAY_OF_MONTH) && tickGreg.get(GregorianCalendar.MONTH) == previousGreg.get(GregorianCalendar.MONTH) && tickGreg.get(GregorianCalendar.YEAR) == previousGreg.get(GregorianCalendar.YEAR)) {
                    tickText = DrawConstants.HOUR_TIME_FORMAT.format(tickValue);
                } else {
                    tickText = DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
                }
            }

            g.setColor(DrawConstants.TICK_LINE_COLOR);
            g.drawLine(x, graphArea.y, x, graphArea.y + graphArea.height + 3);

            g.setColor(DrawConstants.LABEL_TEXT_COLOR);

            int yl = graphArea.y + graphArea.height + 2 + tickTextHeight;
            for (String line : tickText.split("\n")) {
                tickTextBounds = g.getFontMetrics().getStringBounds(line, g);
                tickTextWidth = (int) tickTextBounds.getWidth();
                int xl = x - (tickTextWidth / 2);
                if (xl + tickTextWidth > getWidth() - DrawConstants.getGraphRightSpace()) {
                    xl = getWidth() - DrawConstants.getGraphRightSpace() - tickTextWidth;
                }
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }

        // inform when no data is available
        if (!drawController.hasElementsToBeDrawn()) {
            final String text = DrawConstants.absentText;
            final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
            final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
            final int y = graphArea.y + graphArea.height / 2;

            g.setColor(DrawConstants.LABEL_TEXT_COLOR);
            g.drawString(text, x, y);
        }
        // Log.debug("Time to draw labels: " + (System.currentTimeMillis() -
        // start) + " total time: " + drawLabelsOperarionTime +
        // " total time over running time : " + timeOverTotalTime);
    }

    private void drawVerticalLabels(Graphics2D g, YAxisElement yAxisElement, int leftSide) {
        // draw rectangle hiding to big radio image
        g.setColor(Color.WHITE);
        if (leftSide == 0) {
            g.fillRect(0, DrawConstants.getGraphTopSpace(), DrawConstants.getGraphLeftSpace(), graphArea.height);
            g.fillRect(DrawConstants.getGraphLeftSpace() + graphArea.width, DrawConstants.getGraphTopSpace(), DrawConstants.getTwoAxisGraphRight(), graphArea.height);
        } else {
            g.fillRect(DrawConstants.getGraphLeftSpace() + graphArea.width, DrawConstants.getGraphTopSpace(), DrawConstants.getTwoAxisGraphRight() + DrawConstants.getGraphRightSpace(), graphArea.height);
        }
        String verticalLabel = yAxisElement.getLabel();

        final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
        g.setColor(DrawConstants.LABEL_TEXT_COLOR);
        g.drawString(verticalLabel, (int) (DrawConstants.getGraphLeftSpace() + Math.max((-1 * DrawConstants.getGraphLeftSpace() + 3), -((int) verticalLabelBounds.getWidth() / 2 - 3) + leftSide * ((int) verticalLabelBounds.getWidth() / 2 - 3 + graphArea.width - Math.max(verticalLabelBounds.getWidth() / 2, verticalLabelBounds.getWidth() - DrawConstants.getTwoAxisGraphRight())))), (int) verticalLabelBounds.getHeight());

        double minValue = yAxisElement.getScaledMinValue();
        double maxValue = yAxisElement.getScaledMaxValue();

        double signFactor = 1;
        double useMax = 0;
        if (maxValue < minValue) {
            signFactor = -1;
            double temp = maxValue;
            maxValue = minValue;
            minValue = temp;
            useMax = 1;
        }
        final int sizeSteps = graphArea.height / DrawConstants.getMinVerticalTickSpace();
        int verticalTicks = 2;
        if (sizeSteps >= 4) {
            verticalTicks = 5;
        } else if (verticalTicks >= 2) {
            verticalTicks = 3;
        }
        if (verticalTicks == 0) {
            final int y = graphArea.y + graphArea.height;

            g.setColor(DrawConstants.TICK_LINE_COLOR);
            g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
        } else {
            final double tickDifferenceVertical = (maxValue - minValue) / (verticalTicks - 1);

            for (int i = 0; i < verticalTicks; i++) {
                final double tickValue = minValue + i * tickDifferenceVertical;
                String tickText = DrawConstants.DECIMAL_FORMAT.format(tickValue);

                Double yAxisRatio = yRatios.get(yAxisElement);
                if (yAxisRatio == null) {
                    continue;
                }
                final int y = graphArea.y + graphArea.height - (int) (yAxisRatio * signFactor * (tickValue - (1 - useMax) * minValue - useMax * maxValue));

                g.setColor(DrawConstants.TICK_LINE_COLOR);
                if (leftSide == 0) {
                    g.drawLine(graphArea.x - 3, y, graphArea.x + graphArea.width, y);
                }

                final Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
                final int x = graphArea.x - 6 - (int) bounds.getWidth() + leftSide * (graphArea.width + (int) bounds.getWidth() + 6);
                g.setColor(DrawConstants.LABEL_TEXT_COLOR);
                g.drawString(tickText, x, y + (int) (bounds.getHeight() / 2));
            }
        }
    }

    private void drawMovieLine(final Graphics2D g) {
        if (movieLinePosition < 0 || !drawController.getIntervalAvailable() || graphArea.height < 0) {
            return;
        }

        g.setColor(DrawConstants.MOVIE_FRAME_COLOR);
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
            double minValue = yAxisElement.getScaledMinValue();
            double maxValue = yAxisElement.getScaledMaxValue();

            double ratioY = maxValue < minValue ? graphArea.height / (minValue - maxValue) : graphArea.height / (maxValue - minValue);
            yRatios.put(yAxisElement, ratioY);
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
        final long millis = ((long) ((x - graphArea.x) / ratioX) + interval.getStart().getTime());

        Layers.setTime(new JHVDate(millis));
    }

    // Mouse Input Listener

    @Override
    public void mouseClicked(final MouseEvent e) {
        JHVEvent event = eventModel.getEventAtPosition(new Point(e.getPoint().x - DrawConstants.getGraphLeftSpace(), e.getPoint().y - DrawConstants.getGraphTopSpace()));
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
        mousePosition = null;
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        final Rectangle movieFrame = new Rectangle(movieLinePosition - 3, graphArea.y, 7, graphArea.height);

        mousePressedOnMovieFrame = movieFrame.contains(e.getPoint());
        mousePressedPosition = plotArea.contains(e.getPoint()) ? e.getPoint() : null;
        if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height && !(eventModel.getEventAtPosition(new Point(e.getPoint().x - DrawConstants.getGraphLeftSpace(), e.getPoint().y - DrawConstants.getGraphTopSpace())) != null)) {
            setCursor(UIGlobals.closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (mousePressedPosition != null && mouseDragPosition != null && !mousePressedOnMovieFrame) {

            final int mouseX = e.getX();
            final int mouseY = e.getY();
            double distanceX = -1 * (mouseX - mousePressedPosition.x);
            double distanceY = mouseY - mousePressedPosition.y;
            double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double startTime = plotAreaSpace.getScaledSelectedMinTime() + distanceX / ratioTime;

            double endTime = startTime + graphArea.width / ratioTime;

            plotAreaSpace.setScaledSelectedTime(startTime, endTime, true);
            mouseHelper(distanceY);
        }

        mousePressedPosition = null;
        mouseDragPosition = null;
    }

    public void mouseHelper(double distanceY) {
        Set<ValueSpace> valueSpaces = plotAreaSpace.getValueSpaces();
        for (ValueSpace vs : valueSpaces) {
            Range selectedRange = vs.getSelectedRange();
            double ratioValue = (vs.scale(selectedRange.max) - vs.scale(selectedRange.min)) / graphArea.height;
            double startValue = vs.scale(selectedRange.min) + distanceY * ratioValue;
            double endValue = startValue + graphArea.height * ratioValue;

            vs.setSelectedRange(new Range(vs.invScale(startValue), vs.invScale(endValue)));
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        mouseDragPosition = e.getPoint();

        if (mousePressedPosition != null && mousePressedOnMovieFrame) {
            setMovieFrameManually(mouseDragPosition);
        }

        if (mousePressedPosition != null && !mousePressedOnMovieFrame) {
            setCursor(UIGlobals.closedHandCursor);
            final int mouseX = e.getX();
            final int mouseY = e.getY();
            double distanceX = (-mouseX + mousePressedPosition.x);
            double distanceY = mouseY - mousePressedPosition.y;
            double ratioTime = graphArea.width / (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double startTime = plotAreaSpace.getScaledSelectedMinTime() + distanceX / ratioTime;
            double endTime = startTime + graphArea.width / ratioTime;

            plotAreaSpace.setScaledSelectedTime(startTime, endTime, true);
            mouseHelper(distanceY);

        }
        mousePressedPosition = e.getPoint();
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        mousePosition = new Point(e.getPoint().x - DrawConstants.getGraphLeftSpace(), e.getPoint().y - DrawConstants.getGraphTopSpace());
        final Rectangle frame = new Rectangle(movieLinePosition - 3, graphArea.y, 7, graphArea.height);

        if (movieLinePosition >= 0 && drawController.getIntervalAvailable() && frame.contains(e.getPoint())) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else if (eventModel.getEventAtPosition(new Point(e.getPoint().x - DrawConstants.getGraphLeftSpace(), e.getPoint().y - DrawConstants.getGraphTopSpace())) != null) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            // redrawGraph();
            updateGraph();
            mouseOverEvent = true;
        } else if (e.getPoint().x >= graphArea.x && e.getPoint().x <= graphArea.x + graphArea.width && e.getPoint().y >= graphArea.y && e.getPoint().y <= graphArea.y + graphArea.height) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (mouseOverEvent && (eventModel.getEventAtPosition(new Point(e.getPoint().x - DrawConstants.getGraphLeftSpace(), e.getPoint().y - DrawConstants.getGraphTopSpace())) == null)) {
            mouseOverEvent = false;
            updateGraph();
        }
    }

    // Component Listener

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
        setTwoAxisInformation();
        drawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    // Graph Polyline

    private void setTwoAxisInformation() {
        if (drawController.getYAxisElements().size() >= 2) {
            twoYAxis = 1;
        } else {
            twoYAxis = 0;
        }
    }

    @Override
    public void drawRequest() {
        setTwoAxisInformation();
        forceRedrawGraph = true;
        updateGraph();
    }

    @Override
    public void drawMovieLineRequest(Date time) {
        if (movieTimestamp == null || !movieTimestamp.equals(time)) {
            movieTimestamp = time;
            if (!TimeIntervalLockModel.getInstance().isLocked()) {
                if (updateMovieLineInformation()) {
                    movieLineRequest = true;
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
        double zoomValueFactor = 5;
        Set<ValueSpace> valueSpaces = plotAreaSpace.getValueSpaces();
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollDistance = e.getScrollAmount();
            final int mouseX = e.getX();
            final int mouseY = e.getY();
            boolean inGraphArea = (mouseX >= graphArea.x && mouseX <= graphArea.x + graphArea.width && mouseY > graphArea.y && mouseY <= graphArea.y + graphArea.height);
            boolean inXAxisOrAboveGraph = (mouseX >= graphArea.x && mouseX <= graphArea.x + graphArea.width && (mouseY <= graphArea.y || mouseY >= graphArea.y + graphArea.height));
            boolean inYAxis = (mouseX < graphArea.x || mouseX > graphArea.x + graphArea.width && mouseY > graphArea.y && mouseY <= graphArea.y + graphArea.height);
            if (inGraphArea || inXAxisOrAboveGraph) {
                final double ratioXLeft = (mouseX - graphArea.x) / (double) graphArea.width;
                final double ratioXRight = 1. - ratioXLeft;
                double startTime = plotAreaSpace.getScaledSelectedMinTime();
                double endTime = plotAreaSpace.getScaledSelectedMaxTime();
                if ((!e.isAltDown() && !e.isShiftDown()) || inXAxisOrAboveGraph) {
                    double ratioTime = (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime()) / graphArea.width;
                    startTime = plotAreaSpace.getScaledSelectedMinTime() - scrollValue * zoomTimeFactor * scrollDistance * ratioXLeft * ratioTime;
                    endTime = plotAreaSpace.getScaledSelectedMaxTime() + scrollValue * zoomTimeFactor * scrollDistance * ratioXRight * ratioTime;
                    endTime = Math.min(endTime, plotAreaSpace.getScaledMaxTime());
                } else if (e.isShiftDown()) {
                    double ratioTime = (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime()) / graphArea.width;
                    startTime = plotAreaSpace.getScaledSelectedMinTime() + scrollValue * zoomTimeFactor * scrollDistance * ratioTime;
                    endTime = plotAreaSpace.getScaledSelectedMaxTime() + scrollValue * zoomTimeFactor * scrollDistance * ratioTime;
                }

                plotAreaSpace.setScaledSelectedTime(startTime, endTime, true);
            }
            if (inGraphArea || inYAxis) {
                final double ratioYTop = (mouseY - graphArea.y) / (double) graphArea.height;
                final double ratioYBottom = 1. - ratioYTop;

                for (ValueSpace vs : valueSpaces) {
                    Range selectedRange = vs.getSelectedRange();
                    Range availableRange = vs.getAvailableRange();
                    double startValue = vs.scale(selectedRange.min);
                    double endValue = vs.scale(selectedRange.max);

                    if (((e.isControlDown() || e.isAltDown()) && !e.isShiftDown()) || inYAxis) {
                        double ratioValue = (endValue - startValue) / graphArea.height;

                        endValue = endValue + scrollValue * zoomValueFactor * scrollDistance * ratioYTop * ratioValue;
                        startValue = startValue - scrollValue * zoomValueFactor * scrollDistance * ratioYBottom * ratioValue;
                        startValue = Math.max(vs.scale(availableRange.min), startValue);
                        endValue = Math.min(vs.scale(availableRange.max), endValue);
                    }

                    if (startValue <= endValue /* && startTime <= endTime */&& startValue >= vs.scale(availableRange.min) && startValue <= vs.scale(availableRange.max) && endValue >= vs.scale(availableRange.min) && endValue <= vs.scale(availableRange.max) // &&
                    ) {
                        vs.setSelectedRange(new Range(vs.invScale(startValue), vs.invScale(endValue)));
                    }
                }
            }
        }
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
