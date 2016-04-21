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

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
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
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;

@SuppressWarnings("serial")
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener, MouseWheelListener {

    private final DrawController drawController;
    private Map<YAxisElement, Double> yRatios;
    private long movieTimestamp = Long.MIN_VALUE;
    private int movieLinePosition = -1;
    private Point mousePressedPosition = null;
    private Point mouseDragPosition = null;
    private boolean mousePressedOnMovieFrame = false;
    private Rectangle graphArea = new Rectangle();
    private Rectangle plotArea = new Rectangle();
    private double ratioX = 0;
    private BufferedImage screenImage = null;
    private int twoYAxis = 0;
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
        if (reschedule && !drawController.isLocked()) {
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

        if (width > 0 && height > 0 && sy * (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE + 1) < height && sx * (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + 1) < width && (!movieLineRequest || forceRedrawGraph)) {
            if (width != lastWidth || height != lastHeight) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice device = env.getDefaultScreenDevice();
                GraphicsConfiguration config = device.getDefaultConfiguration();

                screenImage = config.createCompatibleImage(width, height, Transparency.OPAQUE);
                ExportMovie.EVEImage = screenImage;

                lastWidth = width;
                lastHeight = height;
            }

            final Graphics2D g = screenImage.createGraphics();
            AffineTransform tf = g.getTransform();
            tf.preConcatenate(AffineTransform.getScaleInstance(sx, sy));
            g.setTransform(tf);
            drawBackground(g);
            BufferedImage plotPart = screenImage.getSubimage(sx * DrawConstants.GRAPH_LEFT_SPACE, sy * DrawConstants.GRAPH_TOP_SPACE, width - sx * (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + twoYAxis * DrawConstants.TWO_AXIS_GRAPH_RIGHT), height - sy * (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE));
            Graphics2D gplotPart = plotPart.createGraphics();
            gplotPart.setTransform(tf);
            BufferedImage leftAxisPart = screenImage.getSubimage(0, 0, 2 * DrawConstants.GRAPH_LEFT_SPACE, height);
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
                for (DrawableElement de : del) {
                    de.draw(chartg, leftAxisG, plotArea, leftAxisArea, mousePosition);
                }
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
        List<YAxisElement> yAxisElements = drawController.getYAxisElements();
        Interval interval = drawController.getSelectedInterval();
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

        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(DrawConstants.FULL_DATE_TIME_FORMAT.format(new Date(interval.start)), g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        final int tickTextHeight = (int) tickTextBounds.getHeight();
        final int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        final long tickDifferenceHorizontal = (interval.end - interval.start) / (horizontalTickCount - 1);

        GregorianCalendar tickGreg = new GregorianCalendar();
        GregorianCalendar previousGreg = new GregorianCalendar();

        Date previousDate = null;
        for (int i = 0; i < horizontalTickCount; ++i) {
            final Date tickValue = new Date(interval.start + i * tickDifferenceHorizontal);
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
                if (xl + tickTextWidth > getWidth() - DrawConstants.GRAPH_RIGHT_SPACE) {
                    xl = getWidth() - DrawConstants.GRAPH_RIGHT_SPACE - tickTextWidth;
                }
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }

        // inform when no data is available
        if (drawController.getDrawableElements().isEmpty()) {
            final String text = DrawConstants.absentText;
            final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
            final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
            final int y = graphArea.y + graphArea.height / 2;

            g.setColor(DrawConstants.LABEL_TEXT_COLOR);
            g.drawString(text, x, y);
        }
    }

    private void drawVerticalLabels(Graphics2D g, YAxisElement yAxisElement, int leftSide) {
        g.setColor(Color.WHITE);
        if (leftSide == 0) {
            g.fillRect(0, DrawConstants.GRAPH_TOP_SPACE, DrawConstants.GRAPH_LEFT_SPACE, graphArea.height);
            g.fillRect(DrawConstants.GRAPH_LEFT_SPACE + graphArea.width, DrawConstants.GRAPH_TOP_SPACE, DrawConstants.TWO_AXIS_GRAPH_RIGHT, graphArea.height);
        } else {
            g.fillRect(DrawConstants.GRAPH_LEFT_SPACE + graphArea.width, DrawConstants.GRAPH_TOP_SPACE, DrawConstants.TWO_AXIS_GRAPH_RIGHT + DrawConstants.GRAPH_RIGHT_SPACE, graphArea.height);
        }
        String verticalLabel = yAxisElement.getLabel();

        final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
        g.setColor(DrawConstants.LABEL_TEXT_COLOR);
        g.drawString(verticalLabel, (int) (DrawConstants.GRAPH_LEFT_SPACE + Math.max((-1 * DrawConstants.GRAPH_LEFT_SPACE + 3), -((int) verticalLabelBounds.getWidth() / 2 - 3) + leftSide * ((int) verticalLabelBounds.getWidth() / 2 - 3 + graphArea.width - Math.max(verticalLabelBounds.getWidth() / 2, verticalLabelBounds.getWidth() - DrawConstants.TWO_AXIS_GRAPH_RIGHT)))), (int) verticalLabelBounds.getHeight());

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
        final int sizeSteps = graphArea.height / DrawConstants.MIN_VERTICAL_TICK_SPACE;
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
        if (movieLinePosition < 0 || graphArea.height < 0) {
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
        Interval interval = drawController.getSelectedInterval();
        ratioX = graphArea.width / (double) (interval.end - interval.start);
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
        Interval interval = drawController.getSelectedInterval();
        if (movieTimestamp == Long.MIN_VALUE) {
            newMovieLine = -1;
        } else {
            newMovieLine = (int) ((movieTimestamp - interval.start) * ratioX) + graphArea.x;
            if (newMovieLine < graphArea.x || newMovieLine > (graphArea.x + graphArea.width)) {
                newMovieLine = -1;
            }
        }
        if (newMovieLine != movieLinePosition) {
            movieLinePosition = newMovieLine;
            ExportMovie.EVEMovieLinePosition = movieLinePosition;
            return true;
        }
        return false;
    }

    private void setMovieFrameManually(final Point point) {
        Interval interval = drawController.getSelectedInterval();
        if (movieTimestamp == Long.MIN_VALUE) {
            return;
        }
        final int x = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, point.x));
        final long millis = ((long) ((x - graphArea.x) / ratioX) + interval.start);

        Layers.setTime(new JHVDate(millis));
    }

    // Mouse Input Listener

    @Override
    public void mouseClicked(final MouseEvent e) {
        JHVRelatedEvents event = eventModel.getEventUnderMouse();
        Point p = e.getPoint();

        if (event != null) {
            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(event, event.getClosestTo(mouseToTimestamp(new Point(p.x - DrawConstants.GRAPH_LEFT_SPACE, p.y - DrawConstants.GRAPH_TOP_SPACE))));
            dialog.setLocation(e.getLocationOnScreen());
            dialog.validate();
            dialog.pack();
            dialog.setVisible(true);
        } else if (graphArea.contains(p)) {
            setMovieFrameManually(p);
        }
    }

    private long mouseToTimestamp(Point point) {
        Interval interval = drawController.getSelectedInterval();
        int x = Math.max(graphArea.x, Math.min(graphArea.x + graphArea.width, point.x));
        return (long) ((x - graphArea.x) / ratioX) + interval.start;
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
        Point p = e.getPoint();

        mousePressedOnMovieFrame = movieFrame.contains(p);
        mousePressedPosition = plotArea.contains(p) ? p : null;
        if (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y >= graphArea.y && p.y <= graphArea.y + graphArea.height && !(eventModel.getEventAtPosition(new Point(p.x - DrawConstants.GRAPH_LEFT_SPACE, p.y - DrawConstants.GRAPH_TOP_SPACE)) != null)) {
            setCursor(UIGlobals.closedHandCursor);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        Point p = e.getPoint();
        if (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y >= graphArea.y && p.y <= graphArea.y + graphArea.height) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(Cursor.getDefaultCursor());
        }

        if (mousePressedPosition != null && mouseDragPosition != null && !mousePressedOnMovieFrame) {
            double distanceX = mousePressedPosition.x - p.x;
            drawController.selectedAxis.move(graphArea.x, graphArea.width, distanceX);

            double distanceY = p.y - mousePressedPosition.y;
            drawController.setSelectedInterval(false, false);
            mouseHelper(distanceY);
        }

        mousePressedPosition = null;
        mouseDragPosition = null;
    }

    private void mouseHelper(double distanceY) {
        List<YAxisElement> yAxes = drawController.getYAxisElements();
        for (YAxisElement yAxis : yAxes) {
            yAxis.shiftDownPixels(distanceY, graphArea.height);
        }
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        Point p = e.getPoint();

        mouseDragPosition = p;

        if (mousePressedPosition != null && mousePressedOnMovieFrame) {
            setMovieFrameManually(mouseDragPosition);
        }

        if (mousePressedPosition != null && !mousePressedOnMovieFrame) {
            setCursor(UIGlobals.closedHandCursor);
            double distanceX = mousePressedPosition.x - p.x;
            double distanceY = p.y - mousePressedPosition.y;
            drawController.selectedAxis.move(graphArea.x, graphArea.width, distanceX);
            drawController.setSelectedInterval(false, false);
            mouseHelper(distanceY);
        }
        mousePressedPosition = p;
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        final Rectangle frame = new Rectangle(movieLinePosition - 3, graphArea.y, 7, graphArea.height);
        Point p = e.getPoint();

        mousePosition = new Point(p.x - DrawConstants.GRAPH_LEFT_SPACE, p.y - DrawConstants.GRAPH_TOP_SPACE);

        if (movieLinePosition >= 0 && frame.contains(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (EventModel.getSingletonInstance().getEventUnderMouse() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            mouseOverEvent = true;
        } else if (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y >= graphArea.y && p.y <= graphArea.y + graphArea.height) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        if (mouseOverEvent && (eventModel.getEventAtPosition(new Point(p.x - DrawConstants.GRAPH_LEFT_SPACE, p.y - DrawConstants.GRAPH_TOP_SPACE)) == null)) {
            mouseOverEvent = false;
        }
        redrawGraph();
        updateGraph();
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
    public void drawMovieLineRequest(long time) {
        if (movieTimestamp == Long.MIN_VALUE || movieTimestamp != time) {
            movieTimestamp = time;
            if (!drawController.isLocked()) {
                if (updateMovieLineInformation()) {
                    movieLineRequest = true;
                    updateGraph();
                }
            }
        }
        if (time == Long.MIN_VALUE) {
            movieTimestamp = Long.MIN_VALUE;
            if (updateMovieLineInformation()) {
                updateGraph();
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollDistance = e.getWheelRotation() * e.getScrollAmount();
            double zoomTimeFactor = 10;
            List<YAxisElement> yAxisElements = drawController.getYAxisElements();
            final int mouseX = e.getX();
            final int mouseY = e.getY();
            boolean inGraphArea = (mouseX >= graphArea.x && mouseX <= graphArea.x + graphArea.width && mouseY > graphArea.y && mouseY <= graphArea.y + graphArea.height);
            boolean inXAxisOrAboveGraph = (mouseX >= graphArea.x && mouseX <= graphArea.x + graphArea.width && (mouseY <= graphArea.y || mouseY >= graphArea.y + graphArea.height));
            boolean inYAxis = (mouseX < graphArea.x || mouseX > graphArea.x + graphArea.width && mouseY > graphArea.y && mouseY <= graphArea.y + graphArea.height);
            if (inGraphArea || inXAxisOrAboveGraph) {
                if ((!e.isAltDown() && !e.isShiftDown()) || inXAxisOrAboveGraph) {
                    drawController.selectedAxis.zoom(graphArea.x, graphArea.width, mouseX, zoomTimeFactor * scrollDistance);
                    drawController.setSelectedInterval(false, false);
                } else if (e.isShiftDown()) {
                    drawController.selectedAxis.move(graphArea.x, graphArea.width, zoomTimeFactor * scrollDistance);
                    drawController.setSelectedInterval(false, false);
                }
            }
            if (inGraphArea || inYAxis) {
                for (YAxisElement yAxis : yAxisElements) {
                    if (((e.isControlDown() || e.isAltDown()) && !e.isShiftDown()) || inYAxis) {
                        yAxis.zoomSelectedRange(scrollDistance, getHeight() - mouseY - graphArea.y, graphArea.height);
                    }
                }
            }
        }
    }

    private class RedrawTimerTask extends TimerTask {
        @Override
        public void run() {
            if (updateRequestReceived) {
                updateRequestReceived = false;
                timerRedrawGraph();
            }
        }
    }

}
