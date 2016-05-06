package org.helioviewer.jhv.plugins.eveplugin.view.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.events.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.lines.Band;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

@SuppressWarnings("serial")
public class ChartDrawGraphPane extends JComponent implements MouseInputListener, ComponentListener, DrawControllerListener, MouseWheelListener {

    private enum DragMode {
        MOVIELINE, CHART, NODRAG
    }

    private final DrawController drawController;
    private Point mousePressedPosition = null;
    private Point mouseDragPosition = null;

    private BufferedImage screenImage = null;
    private final EventModel eventModel;

    private final Stroke boldStroke = new BasicStroke(2);
    private Point mousePosition;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private boolean toRedraw;

    private DragMode dragMode = DragMode.NODRAG;

    private final Timer redrawTimer = new Timer(1000 / 20, new RedrawListener());

    public ChartDrawGraphPane() {
        setOpaque(true);
        setDoubleBuffered(false);

        toRedraw = false;
        drawController = EVEPlugin.dc;

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        drawController.addDrawControllerListener(this);
        eventModel = EventModel.getSingletonInstance();
        drawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));

        redrawTimer.start();
    }

    private class RedrawListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (toRedraw) {
                toRedraw = false;
                repaint();
            }
        }
    }

    @Override
    public void setVisible(boolean flag) {
        super.setVisible(flag);
        if (flag == true)
            redrawTimer.start();
        else
            redrawTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g1) {
        redrawGraph();

        Graphics2D g = (Graphics2D) g1;
        if (screenImage != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(screenImage, 0, 0, getWidth(), getHeight(), 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
            drawMovieLine(g);
        }
    }

    private void updateGraph() {
        toRedraw = true;
    }

    private void redrawGraph() {
        Rectangle graphSize = drawController.getGraphSize();
        Rectangle graphArea = drawController.getGraphArea();

        int sx = GLInfo.pixelScale[0], sy = GLInfo.pixelScale[1];
        int width = sx * (int) graphSize.getWidth();
        int height = sy * (int) graphSize.getHeight();

        if (width > 0 && height > 0 && sy * (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE + 1) < height && sx * (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + 1) < width) {
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
            drawBackground(g, (int) graphSize.getWidth(), (int) graphSize.getHeight());
            if (graphArea.width > 0 && graphArea.height > 0) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(DrawConstants.font);
                BufferedImage plotPart = screenImage.getSubimage(sx * DrawConstants.GRAPH_LEFT_SPACE, sy * DrawConstants.GRAPH_TOP_SPACE, sx * graphArea.width, sy * graphArea.height);
                Graphics2D gplotPart = plotPart.createGraphics();
                AffineTransform plottf = new AffineTransform();
                plottf.preConcatenate(AffineTransform.getScaleInstance(sx, sy));
                plottf.translate(-graphArea.x, -graphArea.y);
                gplotPart.setTransform(plottf);
                gplotPart.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gplotPart.setFont(DrawConstants.font);
                drawData(g, gplotPart, graphArea, mousePosition);
                gplotPart.dispose();
            }
            g.dispose();
        }
    }

    private void drawData(Graphics2D fullG, Graphics2D plotG, Rectangle graphArea, Point mousePosition) {
        List<LineDataSelectorElement> els = EVEPlugin.ldsm.getAllLineDataSelectorElements();
        for (LineDataSelectorElement el : els) {
            el.draw(plotG, fullG, graphArea, drawController.selectedAxis, mousePosition);
        }
        drawLabels(fullG, graphArea, drawController.selectedAxis);
    }

    private void drawBackground(Graphics2D g, int width, int height) {
        g.setColor(DrawConstants.SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
    }

    private void drawLabels(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis) {
        drawHorizontalLabels(g, graphArea, timeAxis);

        int rightYAxisNumber = -1;
        boolean inLeftYAxis = false;
        boolean inRightYAxes = false;
        if (mousePosition != null) {
            boolean yAxisVerticalCondition = (mousePosition.y > graphArea.y && mousePosition.y <= graphArea.y + graphArea.height);
            inRightYAxes = mousePosition.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
            inLeftYAxis = mousePosition.x < graphArea.x && yAxisVerticalCondition;
            rightYAxisNumber = (mousePosition.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        }
        int ct = -1;
        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    drawVerticalLabels(g, graphArea, el, ct, true);
                } else {
                    drawVerticalLabels(g, graphArea, el, ct, false);
                }
                ct++;
            }
        }
        if (ct == -1) {
            drawNoData(g, graphArea);
            return;
        }

        drawTimelineValues(g, graphArea, timeAxis);
    }

    private void drawTimelineValues(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis) {
        if (mousePosition == null || !graphArea.contains(mousePosition))
            return;
        long ts = timeAxis.pixel2value(graphArea.x, graphArea.width, mousePosition.x);
        String lbl = "(" + TimeUtils.utcDateFormat.format(ts);
        int currWidth = 0;
        g.setColor(Color.BLACK);
        g.drawString(lbl, graphArea.width / 2 + currWidth, DrawConstants.GRAPH_TOP_SPACE / 2);
        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(lbl, g);
        currWidth += (int) tickTextBounds.getWidth();

        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            if (el.isVisible() && el instanceof Band) {
                lbl = ", ";
                g.setColor(Color.BLACK);
                g.drawString(lbl, graphArea.width / 2 + currWidth, DrawConstants.GRAPH_TOP_SPACE / 2);
                tickTextBounds = g.getFontMetrics().getStringBounds(lbl, g);
                currWidth += (int) tickTextBounds.getWidth();

                Band band = (Band) el;
                lbl = band.getStringValue(ts);
                g.setColor(band.getDataColor());
                g.drawString(lbl, graphArea.width / 2 + currWidth, DrawConstants.GRAPH_TOP_SPACE / 2);
                tickTextBounds = g.getFontMetrics().getStringBounds(lbl, g);
                currWidth += (int) tickTextBounds.getWidth();
            }
        }
        g.setColor(Color.BLACK);
        lbl = ")";
        g.drawString(lbl, graphArea.width / 2 + currWidth, DrawConstants.GRAPH_TOP_SPACE / 2);
    }

    private void drawHorizontalLabels(Graphics2D g, Rectangle graphArea, TimeAxis xAxis) {
        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(DrawConstants.FULL_DATE_TIME_FORMAT.format(new Date(xAxis.start)), g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        int tickTextHeight = (int) tickTextBounds.getHeight();
        int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        long tickDifferenceHorizontal = (xAxis.end - xAxis.start) / (horizontalTickCount - 1);

        long previousDate = Long.MIN_VALUE;
        for (int i = 0; i < horizontalTickCount; ++i) {
            final long tickValue = xAxis.start + i * tickDifferenceHorizontal;
            final int x = drawController.selectedAxis.value2pixel(graphArea.x, graphArea.width, tickValue);
            final String tickText;
            if (previousDate == Long.MIN_VALUE) {
                tickText = DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
            } else {
                long tickDayNumber = tickValue / TimeUtils.DAY_IN_MILLIS;
                long prevDayNumber = previousDate / TimeUtils.DAY_IN_MILLIS;

                if (tickDayNumber == prevDayNumber) {
                    tickText = DrawConstants.HOUR_TIME_FORMAT.format(tickValue);
                } else {
                    tickText = DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE.format(tickValue);
                }
            }
            g.setColor(DrawConstants.TICK_LINE_COLOR);
            g.drawLine(x, graphArea.y, x, graphArea.y + graphArea.height + 3);

            g.setColor(Color.BLACK);
            int yl = graphArea.y + graphArea.height + 2 + tickTextHeight;
            for (String line : tickText.split("\n")) {
                tickTextBounds = g.getFontMetrics().getStringBounds(line, g);
                tickTextWidth = (int) tickTextBounds.getWidth();
                int xl = x - (tickTextWidth / 2);

                int xend = (int) drawController.getGraphSize().getWidth() - DrawConstants.GRAPH_RIGHT_SPACE - tickTextWidth;
                if (xl > xend) {
                    xl = xend;
                }
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }
    }

    private void drawNoData(Graphics2D g, Rectangle graphArea) {
        final String text = DrawConstants.absentText;
        final int textWidth = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
        final int x = graphArea.x + (graphArea.width / 2) - (textWidth / 2);
        final int y = graphArea.y + graphArea.height / 2;

        g.setColor(DrawConstants.LABEL_TEXT_COLOR);
        g.drawString(text, x, y);
    }

    private void drawVerticalLabels(Graphics2D g, Rectangle graphArea, LineDataSelectorElement el, int leftSide, boolean highlight) {
        int axis_x_offset;
        g.setColor(Color.WHITE);
        if (leftSide == -1) {
            axis_x_offset = graphArea.x;
        } else {
            axis_x_offset = graphArea.x + graphArea.width + (leftSide) * DrawConstants.RIGHT_AXIS_WIDTH;
        }

        g.setColor(el.getDataColor());
        YAxis yAxis = el.getYAxis();

        // Vertical lines
        {
            double start = yAxis.pixel2ScaledValue(graphArea.y, graphArea.height, graphArea.y + graphArea.height);
            double end = yAxis.pixel2ScaledValue(graphArea.y, graphArea.height, graphArea.y);
            if (start > end) {
                double temp = start;
                start = end;
                end = temp;
            }
            int decade = (int) Math.floor(Math.log10(end - start));
            double step = Math.pow(10, decade);
            double startv = (Math.floor(start / step)) * step;
            double endv = (Math.ceil(end / step)) * step;
            if ((endv - startv) / step < 5) {
                step = step / 2;
            }
            double tick = startv;
            int ct = 0;
            drawHorizontalTickline(g, graphArea, yAxis, start, axis_x_offset, leftSide, false, highlight);
            while (tick <= endv && ct < 20) {
                if (tick >= start && tick <= end)
                    drawHorizontalTickline(g, graphArea, yAxis, tick, axis_x_offset, leftSide, true, highlight);
                tick += step;
                ct++;
            }
            drawHorizontalTickline(g, graphArea, yAxis, end, axis_x_offset, leftSide, false, highlight);
        }

        // Label and axis
        {
            String verticalLabel = yAxis.getLabel();
            final Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
            int vWidth = (int) verticalLabelBounds.getWidth();
            int vHeight = (int) verticalLabelBounds.getHeight();
            int labelCompensation = vWidth / 2;
            if (highlight) {
                Stroke s = g.getStroke();
                g.setStroke(boldStroke);
                g.setFont(DrawConstants.fontBold);
                g.drawLine(axis_x_offset, graphArea.y, axis_x_offset, graphArea.y + graphArea.height + 3);
                g.drawString(verticalLabel, axis_x_offset - labelCompensation, vHeight);
                g.setStroke(s);
                g.setFont(DrawConstants.font);
            } else {
                g.drawLine(axis_x_offset, graphArea.y, axis_x_offset, graphArea.y + graphArea.height + 3);
                g.drawString(verticalLabel, axis_x_offset - labelCompensation, vHeight);
            }
        }
    }

    private void drawHorizontalTickline(Graphics g, Rectangle graphArea, YAxis yAxis, double tick, int axis_x_offset, int leftSide, boolean needTxt, boolean highlight) {
        String tickText = DrawConstants.valueFormatter.format(tick);
        int y = yAxis.scaledvalue2pixel(graphArea.y, graphArea.height, tick);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
        int x_str;
        if (leftSide == -1) {
            x_str = axis_x_offset - 6 - (int) bounds.getWidth();
            g.drawLine(axis_x_offset - 3, y, graphArea.x + graphArea.width, y);
        } else {
            x_str = axis_x_offset;
        }
        if (needTxt) {
            if (highlight) {
                g.setFont(DrawConstants.fontBold);
                g.drawString(tickText, x_str, y + (int) (bounds.getHeight() / 2));
                g.setFont(DrawConstants.font);
            } else {
                g.drawString(tickText, x_str, y + (int) (bounds.getHeight() / 2));
            }
        }
    }

    private void drawMovieLine(Graphics2D g) {
        int movieLinePosition = drawController.getMovieLinePosition();
        ExportMovie.EVEMovieLinePosition = movieLinePosition;
        if (movieLinePosition < 0) {
            return;
        }
        g.setColor(DrawConstants.MOVIE_FRAME_COLOR);
        g.drawLine(movieLinePosition, 0, movieLinePosition, (int) drawController.getGraphSize().getHeight());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (e.getClickCount() == 2) {
            doubleClicked(p);
            return;
        }

        JHVRelatedEvents event = eventModel.getEventUnderMouse();
        if (event != null) {
            Rectangle graphArea = drawController.getGraphArea();
            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(event, event.getClosestTo(drawController.selectedAxis.pixel2value(graphArea.x, graphArea.width, p.x)));
            dialog.setLocation(e.getLocationOnScreen());
            dialog.validate();
            dialog.pack();
            dialog.setVisible(true);
        } else {
            drawController.setMovieFrame(p);
        }
    }

    private void doubleClicked(Point p) {
        drawController.resetAxis(p);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mousePosition = null;
        updateGraph();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        mousePressedPosition = p;
        if (overMovieLine(p/*, drawController.getGraphArea()*/)) {
            // setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            dragMode = DragMode.MOVIELINE;
        } else {
            setCursor(UIGlobals.closedHandCursor);
            dragMode = DragMode.CHART;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();

        switch (dragMode) {
        case CHART:
            setCursor(UIGlobals.openHandCursor);
            if (mousePressedPosition != null && mouseDragPosition != null) {
                drawController.moveX(mousePressedPosition.x - p.x);
                drawController.moveAllAxes(p.y - mousePressedPosition.y);
            }
            break;
        case MOVIELINE:
            drawController.setMovieFrame(p);
            break;
        default:
            break;
        }
        dragMode = DragMode.NODRAG;
        mousePressedPosition = null;
        mouseDragPosition = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        mouseDragPosition = p;
        if (mousePressedPosition != null) {
            switch (dragMode) {
            case CHART:
                setCursor(UIGlobals.closedHandCursor);
                drawController.moveX(mousePressedPosition.x - p.x);
                drawController.moveY(p, p.y - mousePressedPosition.y);
                break;
            case MOVIELINE:
                drawController.setMovieFrame(p);
                break;
            default:
                break;
            }
        }
        mousePressedPosition = p;
    }

    private boolean overMovieLine(Point p/*, Rectangle graphArea*/) {
        int movieLinePosition = drawController.getMovieLinePosition();
        // Rectangle frame = new Rectangle(movieLinePosition - 3, graphArea.y, 7, graphArea.height);
        return movieLinePosition >= 0 && movieLinePosition - 3 <= p.x && p.x <= movieLinePosition + 3;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Rectangle graphArea = drawController.getGraphArea();
        mousePosition = e.getPoint();
        if (overMovieLine(mousePosition/*, graphArea*/)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (EventModel.getSingletonInstance().getEventUnderMouse() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (mousePosition.x >= graphArea.x && mousePosition.x <= graphArea.x + graphArea.width && mousePosition.y >= graphArea.y && mousePosition.y <= graphArea.y + graphArea.height) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(Cursor.getDefaultCursor());
        }

        updateGraph();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            Point p = e.getPoint();
            int scrollDistance = e.getWheelRotation() * e.getScrollAmount();
            drawController.zoomXY(p, scrollDistance, e.isShiftDown(), e.isAltDown(), e.isControlDown());
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // only resize called
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // only resize called
    }

    @Override
    public void componentResized(ComponentEvent e) {
        drawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // only resize called
    }

    @Override
    public void drawRequest() {
        updateGraph();
    }

    @Override
    public void drawMovieLineRequest() {
        updateGraph();
    }

    @Override
    public void movieIntervalChanged(long start, long end) {
    }

}
