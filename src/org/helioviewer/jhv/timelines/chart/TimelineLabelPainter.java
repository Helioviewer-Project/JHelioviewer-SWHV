package org.helioviewer.jhv.timelines.chart;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

final class TimelineLabelPainter {

    private final Stroke boldStroke = new BasicStroke(2);
    private final Stroke thinStroke = new BasicStroke(0.5f);

    void drawStaticLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        Stroke stroke = g.getStroke();
        try {
            g.setStroke(thinStroke);
            g.setFont(DrawConstants.font);
            if (geometry.isStacked()) {
                drawStackedLabels(g, geometry, xAxis);
            } else {
                drawTimeLabels(g, geometry, xAxis);
                drawYAxisLabels(g, geometry);
            }
        } finally {
            g.setStroke(stroke);
        }
    }

    void drawMouseValues(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis, Point mousePosition) {
        g.setFont(DrawConstants.font);
        Rectangle graphArea = geometry.area();
        if (mousePosition == null || !graphArea.contains(mousePosition))
            return;

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

    private void drawTimeLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        drawHorizontalLabels(g, geometry, xAxis, 0, null);
        TimelineLayers.forEachPropagated((tl, row) -> drawHorizontalLabels(g, geometry, xAxis, row + 1, tl));
    }

    private void drawYAxisLabels(Graphics2D g, GraphGeometry geometry) {
        TimelineLayers.forEachYAxis((tl, axisIndex) -> drawVerticalLabels(g, geometry, tl, axisIndex, tl.getYAxis().isHighlighted()));
    }

    private void drawStackedLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        List<TimelineLayer> visibleLayers = TimelineLayers.getVisibleYAxisLayers();
        List<Rectangle> layerAreas = geometry.getLayerAreas();

        drawHorizontalLabels(g, geometry, xAxis, 0, null);

        for (int i = 0; i < visibleLayers.size() && i < layerAreas.size(); i++) {
            TimelineLayer tl = visibleLayers.get(i);
            Rectangle stripArea = layerAreas.get(i);

            drawStackedVerticalLabels(g, geometry, stripArea, tl);
        }

        g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
        for (int i = 1; i < layerAreas.size(); i++) {
            Rectangle prev = layerAreas.get(i - 1);
            int sepY = prev.y + prev.height + 1;
            g.drawLine(geometry.area().x, sepY, geometry.graphRight(), sepY);
        }
    }

    private void drawStackedVerticalLabels(Graphics2D g, GraphGeometry geometry, Rectangle stripArea, TimelineLayer tl) {
        int axisX = stripArea.x;

        g.setColor(tl.getDataColor());
        YAxis yAxis = tl.getYAxis();
        YAxis.Mapper yMapper = geometry.yMapper(yAxis, stripArea);
        YAxis.Ticks ticks = yAxis.ticks(yMapper);

        drawStackedHorizontalTickline(g, stripArea, yMapper, ticks.start(), axisX, false);
        int count = 0;
        for (double tick = ticks.first(); tick <= ticks.last() && count < 20; tick += ticks.step(), count++) {
            if (ticks.start() <= tick && tick <= ticks.end()) {
                drawStackedHorizontalTickline(g, stripArea, yMapper, tick, axisX, true);
            }
        }
        drawStackedHorizontalTickline(g, stripArea, yMapper, ticks.end(), axisX, false);

        g.drawLine(axisX, stripArea.y, axisX, stripArea.y + stripArea.height);
        drawRotatedLabel(g, yAxis.getLabel(), axisX, stripArea);
    }

    private static void drawStackedHorizontalTickline(Graphics g, Rectangle stripArea, YAxis.Mapper yMapper, double tick, int axisX, boolean needTxt) {
        String tickText = DrawConstants.valueFormatter.format(tick);
        int y = yMapper.scaledToPixel(tick);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);

        java.awt.Color lineColor = g.getColor();
        g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
        g.drawLine(axisX + 1, y, stripArea.x + stripArea.width, y);
        g.setColor(lineColor);

        if (needTxt) {
            int xText = axisX - 6 - (int) bounds.getWidth();
            g.drawString(tickText, xText, y + (int) (bounds.getHeight() / 2));
        }
    }

    private static int drawString(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x, y);
        return (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
    }

    private static void drawRotatedLabel(Graphics2D g, String label, int axisX, Rectangle stripArea) {
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(label, g);
        double textWidth = bounds.getWidth();
        double textHeight = bounds.getHeight();

        double cx = axisX / 4.0;
        double cy = stripArea.y + stripArea.height / 2.0;

        AffineTransform oldTransform = g.getTransform();
        try {
            g.translate(cx, cy);
            g.rotate(-Math.PI / 2);
            g.drawString(label, (float) (-textWidth / 2), (float) (textHeight / 3));
        } finally {
            g.setTransform(oldTransform);
        }
    }

    private static void drawHorizontalLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis, int row, TimelineLayer tl) {
        Rectangle graphArea = geometry.area();
        String tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT, xAxis.start());
        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(tickText, g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        int tickTextHeight = (int) tickTextBounds.getHeight() + row * DrawConstants.GRAPH_BOTTOM_AXIS_SPACE;
        int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        long tickDifferenceHorizontal = (xAxis.end() - xAxis.start()) / (horizontalTickCount - 1);
        long start = tl == null ? xAxis.start() : tl.getObservationTime(xAxis.start());
        long end = tl == null ? xAxis.end() : tl.getObservationTime(xAxis.end());
        TimeAxis.Mapper xMapper = new TimeAxis.Mapper(start, end, graphArea.x, graphArea.width);

        long previousDate = Long.MIN_VALUE;
        for (int i = 0; i < horizontalTickCount; ++i) {
            long tickValue = xAxis.start() + i * tickDifferenceHorizontal;
            if (tl != null) {
                tickValue = tl.getObservationTime(tickValue);
            }
            int x = xMapper.toPixel(tickValue);

            if (tl == null) {
                g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
                g.drawLine(x, graphArea.y, x, geometry.graphBottom() + 3);
                g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
            } else {
                g.setColor(tl.getDataColor());
            }
            drawTimeText(g, geometry, x, formatTickText(tickValue, previousDate), tickTextHeight);
            previousDate = tickValue;
        }
    }

    private static String formatTickText(long tickValue, long previousDate) {
        if (previousDate == Long.MIN_VALUE) {
            return TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
        }

        long tickDayNumber = tickValue / TimeUtils.DAY_IN_MILLIS;
        long prevDayNumber = previousDate / TimeUtils.DAY_IN_MILLIS;
        return TimeUtils.format(tickDayNumber == prevDayNumber ? DrawConstants.HOUR_TIME_FORMAT : DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
    }

    private static void drawTimeText(Graphics2D g, GraphGeometry geometry, int x, String tickText, int tickTextHeight) {
        int y = geometry.graphBottom() + 2 + tickTextHeight;
        Iterator<String> lines = tickText.lines().iterator();
        while (lines.hasNext()) {
            String line = lines.next();
            int lineWidth = (int) g.getFontMetrics().getStringBounds(line, g).getWidth();
            int xl = Math.min(x - lineWidth / 2, geometry.rightEdge() - lineWidth);
            g.drawString(line, xl, y);
            y += g.getFontMetrics().getHeight() * 2 / 3;
        }
    }

    private void drawVerticalLabels(Graphics2D g, GraphGeometry geometry, TimelineLayer tl, int leftSide, boolean highlight) {
        Rectangle graphArea = geometry.area();
        int axisX = graphArea.x;
        if (leftSide != -1) {
            axisX += graphArea.width + leftSide * DrawConstants.RIGHT_AXIS_WIDTH;
        }

        g.setColor(tl.getDataColor());
        YAxis yAxis = tl.getYAxis();
        YAxis.Mapper yMapper = geometry.yMapper(yAxis);
        YAxis.Ticks ticks = yAxis.ticks(yMapper);

        drawHorizontalTickline(g, graphArea, yMapper, ticks.start(), axisX, leftSide, false, highlight);
        int count = 0;
        for (double tick = ticks.first(); tick <= ticks.last() && count < 20; tick += ticks.step(), count++) {
            if (ticks.start() <= tick && tick <= ticks.end()) {
                drawHorizontalTickline(g, graphArea, yMapper, tick, axisX, leftSide, true, highlight);
            }
        }
        drawHorizontalTickline(g, graphArea, yMapper, ticks.end(), axisX, leftSide, false, highlight);

        drawVerticalTitle(g, graphArea, axisX, yAxis.getLabel(), highlight);
    }

    private void drawVerticalTitle(Graphics2D g, Rectangle graphArea, int axisX, String verticalLabel, boolean highlight) {
        Rectangle2D verticalLabelBounds = g.getFontMetrics().getStringBounds(verticalLabel, g);
        int vWidth = (int) verticalLabelBounds.getWidth();
        int vHeight = (int) verticalLabelBounds.getHeight();
        int labelCompensation = vWidth / 2;

        Stroke stroke = g.getStroke();
        if (highlight) {
            g.setStroke(boldStroke);
            g.setFont(DrawConstants.fontBold);
        }

        g.drawLine(axisX, graphArea.y, axisX, graphArea.y + graphArea.height + 3);
        g.drawString(verticalLabel, axisX - labelCompensation, vHeight);

        if (highlight) {
            g.setStroke(stroke);
            g.setFont(DrawConstants.font);
        }
    }

    private static void drawHorizontalTickline(Graphics g, Rectangle graphArea, YAxis.Mapper yMapper, double tick, int axisX, int leftSide, boolean needTxt, boolean highlight) {
        String tickText = DrawConstants.valueFormatter.format(tick);
        int y = yMapper.scaledToPixel(tick);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
        int xText;
        if (leftSide == -1) {
            xText = axisX - 6 - (int) bounds.getWidth();
            java.awt.Color lineColor = g.getColor();
            g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
            g.drawLine(axisX - 3, y, graphArea.x + graphArea.width, y);
            g.setColor(lineColor);
        } else {
            xText = axisX;
        }
        if (needTxt) {
            if (highlight) {
                g.setFont(DrawConstants.fontBold);
            }
            g.drawString(tickText, xText, y + (int) (bounds.getHeight() / 2));
            if (highlight) {
                g.setFont(DrawConstants.font);
            }
        }
    }

}
