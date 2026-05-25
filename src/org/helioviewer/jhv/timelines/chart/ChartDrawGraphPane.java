package org.helioviewer.jhv.timelines.chart;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.GraphGeometry.YAxisHit;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

@SuppressWarnings("serial")
final class ChartDrawGraphPane extends JComponent implements MouseInputListener, MouseWheelListener, ComponentListener, DrawController.Listener {

    private enum DragMode {
        MOVIELINE, CHART, NODRAG
    }

    private Point mousePressedPosition;
    private Point mouseDragPosition;

    private BufferedImage screenImage;

    private final Stroke boldStroke = new BasicStroke(2);
    private final Stroke thinStroke = new BasicStroke(0.5f);
    private Point mousePosition;
    private int lastWidth = -1;
    private int lastHeight = -1;

    private boolean redrawGraphArea;

    private DragMode dragMode = DragMode.NODRAG;

    ChartDrawGraphPane() {
        setPreferredSize(new Dimension(-1, 50));
        setOpaque(true);
        setDoubleBuffered(false);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        DrawController.addDrawListener(this);
        DrawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void removeNotify() {
        DrawController.removeDrawListener(this);
        super.removeNotify();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            ExportMovie.EVEImage = screenImage;
            DrawController.start();
        } else {
            ExportMovie.EVEImage = null;
            DrawController.stop();
        }
    }

    private boolean toggleAxisHighlight(GraphGeometry geometry) {
        YAxisHit hit = mousePosition == null ? null : geometry.yAxisHit(mousePosition);

        boolean toggled = false;
        int ct = -1;
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.showYAxis()) {
                if (hit != null && hit.targets(ct)) {
                    toggled = toggled || !tl.getYAxis().isHighlighted();
                    tl.getYAxis().setHighlighted(true);
                } else {
                    toggled = toggled || tl.getYAxis().isHighlighted();
                    tl.getYAxis().setHighlighted(false);
                }
                ct++;
            }
        }
        return toggled;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        GraphGeometry geometry = DrawController.getGeometry();
        Rectangle graphArea = geometry.area();

        boolean axisHighlightChanged = toggleAxisHighlight(geometry);
        if (redrawGraphArea || axisHighlightChanged) {
            redrawGraphArea = false;
            redrawGraph(geometry);
        }

        Graphics2D g = (Graphics2D) g1;
        if (screenImage != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(DrawConstants.font);
            g.drawImage(screenImage, 0, 0, getWidth(), getHeight(), null);
            drawMovieLine(g);
            drawTimelineValues(g, geometry, DrawController.selectedAxis);
        }
    }

    private void redrawGraph(GraphGeometry geometry) {
        Rectangle graphArea = geometry.area();
        Rectangle graphSize = geometry.size();
        double sx = Display.pixelScale[0], sy = Display.pixelScale[1];
        int width = (int) (sx * graphSize.getWidth() + .5);
        int height = (int) (sy * graphSize.getHeight() + .5);

        if (width > 0 && height > 0) {
            if (width != lastWidth || height != lastHeight) {
                screenImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height, Transparency.OPAQUE);
                ExportMovie.EVEImage = screenImage;

                lastWidth = width;
                lastHeight = height;
            }

            Graphics2D fullG = screenImage.createGraphics();
            drawBackground(fullG, screenImage.getWidth(), screenImage.getHeight());

            fullG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            fullG.setFont(DrawConstants.font);
            fullG.setTransform(AffineTransform.getScaleInstance(sx, sy));

            Graphics2D plotG = (Graphics2D) fullG.create();
            plotG.setClip(graphArea);
            TimeAxis xAxis = DrawController.selectedAxis;
            TimelineLayers.get().forEach(layer -> layer.draw(plotG, graphArea, xAxis, mousePosition));
            drawLabels(fullG, geometry, xAxis);

            plotG.dispose();
            fullG.dispose();
        }
    }

    private static void drawBackground(Graphics2D g, int width, int height) {
        g.setColor(UIGlobals.TL_SELECTED_INTERVAL_BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
    }

    private void drawLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        Rectangle graphArea = geometry.area();
        Stroke stroke = g.getStroke();
        g.setStroke(thinStroke);
        int ht = 0;
        drawHorizontalLabels(g, geometry, xAxis, ht, null);
        ht++;
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.isPropagated()) {
                g.setColor(tl.getDataColor());
                drawHorizontalLabels(g, geometry, xAxis, ht, tl);
                ht++;
            }
        }

        int ct = -1;
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.showYAxis()) {
                drawVerticalLabels(g, geometry, tl, ct, tl.getYAxis().isHighlighted());
                ct++;
            }
        }
        g.setStroke(stroke);
    }

    private void drawTimelineValues(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        Rectangle graphArea = geometry.area();
        if (mousePosition == null || !graphArea.contains(mousePosition)) {
            return;
        }
        long ts = geometry.xMapper(xAxis).toValue(mousePosition.x);
        int x = graphArea.width / 2;
        int y = DrawConstants.GRAPH_TOP_SPACE / 2;

        g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
        int currWidth = drawString(g, "(" + TimeUtils.format(TimeUtils.sqlTimeFormatter, ts), x, y);

        for (TimelineLayer tl : TimelineLayers.get()) {
            if (!tl.isEnabled()) {
                continue;
            }

            String value = tl.getStringValue(ts);
            if (value != null) {
                g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
                currWidth += drawString(g, ", ", x + currWidth, y);

                g.setColor(tl.getDataColor());
                currWidth += drawString(g, value, x + currWidth, y);
            }
        }

        g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
        drawString(g, ")", x + currWidth, y);
    }

    private static int drawString(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x, y);
        return (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
    }

    private static void drawHorizontalLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis, int ht, TimelineLayer tl) {
        Rectangle graphArea = geometry.area();
        String tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT, xAxis.start());
        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(tickText, g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        int tickTextHeight = (int) tickTextBounds.getHeight() + ht * DrawConstants.GRAPH_BOTTOM_AXIS_SPACE;
        int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        int xend = geometry.rightEdge();
        TimeAxis.Mapper xMapper;
        if (tl == null) {
            xMapper = geometry.xMapper(xAxis);
        } else {
            xMapper = new TimeAxis.Mapper(tl.getObservationTime(xAxis.start()), tl.getObservationTime(xAxis.end()), graphArea.x, graphArea.width);
        }
        long tickDifferenceHorizontal = (xAxis.end() - xAxis.start()) / (horizontalTickCount - 1);

        long previousDate = Long.MIN_VALUE;
        for (int i = 0; i < horizontalTickCount; ++i) {
            long tickValue = xAxis.start() + i * tickDifferenceHorizontal;
            if (tl != null) {
                tickValue = tl.getObservationTime(tickValue);
            }

            int x = xMapper.toPixel(tickValue);
            if (previousDate == Long.MIN_VALUE) {
                tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
            } else {
                long tickDayNumber = tickValue / TimeUtils.DAY_IN_MILLIS;
                long prevDayNumber = previousDate / TimeUtils.DAY_IN_MILLIS;
                tickText = TimeUtils.format(tickDayNumber == prevDayNumber ? DrawConstants.HOUR_TIME_FORMAT : DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
            }

            if (tl == null) {
                g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
                g.drawLine(x, graphArea.y, x, graphArea.y + graphArea.height + 3);
                g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
            } else {
                g.setColor(tl.getDataColor());
            }

            int yl = graphArea.y + graphArea.height + 2 + tickTextHeight;
            for (String line : Regex.Return.split(tickText)) {
                tickTextBounds = g.getFontMetrics().getStringBounds(line, g);
                tickTextWidth = (int) tickTextBounds.getWidth();
                int xl = x - (tickTextWidth / 2);
                if (xl > xend - tickTextWidth) {
                    xl = xend - tickTextWidth;
                }
                g.drawString(line, xl, yl);
                yl += g.getFontMetrics().getHeight() * 2 / 3;
            }

            previousDate = tickValue;
        }
    }

    private void drawVerticalLabels(Graphics2D g, GraphGeometry geometry, TimelineLayer tl, int leftSide, boolean highlight) {
        Rectangle graphArea = geometry.area();
        int axis_x_offset = graphArea.x;
        if (leftSide != -1) {
            axis_x_offset += graphArea.width + leftSide * DrawConstants.RIGHT_AXIS_WIDTH;
        }

        g.setColor(tl.getDataColor());
        YAxis yAxis = tl.getYAxis();
        YAxis.Mapper yMapper = geometry.yMapper(yAxis);
        double start = yMapper.pixelToScaled(geometry.graphBottom());
        double end = yMapper.pixelToScaled(graphArea.y);
        if (start > end) {
            double temp = start;
            start = end;
            end = temp;
        }
        int decade = (int) Math.floor(Math.log10(end - start));
        double step = Math.pow(10, decade);
        double startv = Math.floor(start / step) * step;
        double endv = Math.ceil(end / step) * step;
        if ((endv - startv) / step < 5) {
            step /= 2;
        }
        double tick = startv;
        int ct = 0;
        drawHorizontalTickline(g, graphArea, yMapper, start, axis_x_offset, leftSide, false, highlight);
        while (tick <= endv && ct < 20) {
            if (tick >= start && tick <= end) {
                drawHorizontalTickline(g, graphArea, yMapper, tick, axis_x_offset, leftSide, true, highlight);
            }
            tick += step;
            ct++;
        }
        drawHorizontalTickline(g, graphArea, yMapper, end, axis_x_offset, leftSide, false, highlight);

        String verticalLabel = yAxis.getLabel();
        Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
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

    private static void drawHorizontalTickline(Graphics g, Rectangle graphArea, YAxis.Mapper yMapper, double tick, int axis_x_offset, int leftSide, boolean needTxt, boolean highlight) {
        String tickText = DrawConstants.valueFormatter.format(tick);
        int y = yMapper.scaledToPixel(tick);
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

    private static void drawMovieLine(Graphics2D g) {
        int movieLinePosition = DrawController.getMovieLinePosition();
        ExportMovie.EVEMovieLinePosition = movieLinePosition;
        if (movieLinePosition < 0) {
            return;
        }
        g.setColor(UIGlobals.TL_MOVIE_FRAME_COLOR);
        g.drawLine(movieLinePosition, 0, movieLinePosition, DrawController.getGeometry().size().height);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (e.getClickCount() == 2) {
            DrawController.resetAxis(p);
            return;
        }

        ClickableDrawable element = TimelineLayers.getDrawableUnderMouse();
        if (element != null) {
            element.clicked(e.getLocationOnScreen(), DrawController.getGeometry().xMapper(DrawController.selectedAxis).toValue(p.x));
        } else {
            DrawController.setMovieFrame(p);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        JHVEventCache.highlight(null);
        mousePosition = null;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        mousePressedPosition = p;
        if (overMovieLine(p)) {
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
            case CHART -> {
                setCursor(UIGlobals.openHandCursor);
                if (mousePressedPosition != null && mouseDragPosition != null) {
                    DrawController.moveX(mousePressedPosition.x - p.x);
                    DrawController.moveAllAxes(p.y - mousePressedPosition.y);
                }
            }
            case MOVIELINE -> DrawController.setMovieFrame(p);
            case NODRAG -> {}
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
                case CHART -> {
                    setCursor(UIGlobals.closedHandCursor);
                    DrawController.moveX(mousePressedPosition.x - p.x);
                    DrawController.moveY(p, p.y - mousePressedPosition.y);
                }
                case MOVIELINE -> DrawController.setMovieFrame(p);
                case NODRAG -> {}
            }
        }
        mousePressedPosition = p;
    }

    private static boolean overMovieLine(Point p) {
        int movieLinePosition = DrawController.getMovieLinePosition();
        return movieLinePosition >= 0 && movieLinePosition - 3 <= p.x && p.x <= movieLinePosition + 3;
    }

    private static boolean highlightChanged(Point p) {
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.highLightChanged(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = e.getPoint();
        if (overMovieLine(mousePosition)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (TimelineLayers.getDrawableUnderMouse() != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (DrawController.getGeometry().area().contains(mousePosition)) {
            setCursor(UIGlobals.openHandCursor);
        } else {
            setCursor(Cursor.getDefaultCursor());
        }

        if (highlightChanged(mousePosition)) {
            drawRequest();
        } else {
            repaint(); // for timeline values
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int scrollDistance = e.getWheelRotation() * e.getScrollAmount();
            DrawController.zoomXY(e.getPoint(), scrollDistance, e.isShiftDown(), e.isAltDown(), e.isControlDown());
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {}

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentResized(ComponentEvent e) {
        DrawController.setGraphInformation(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void drawRequest() {
        redrawGraphArea = true;
        repaint();
    }

    @Override
    public void drawMovieLineRequest() {
        repaint();
    }

}
