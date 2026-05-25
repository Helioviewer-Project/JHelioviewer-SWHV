package org.helioviewer.jhv.timelines.chart;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

final class TimelineLabelPainter {

    private final Stroke boldStroke = new BasicStroke(2);
    private final Stroke thinStroke = new BasicStroke(0.5f);

    void drawStaticLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        Stroke stroke = g.getStroke();
        g.setStroke(thinStroke);

        drawTimeLabels(g, geometry, xAxis);
        drawYAxisLabels(g, geometry);

        g.setStroke(stroke);
    }

    void drawMouseValues(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis, Point mousePosition) {
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

    private void drawTimeLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis) {
        drawHorizontalLabels(g, geometry, xAxis, 0, null);

        TimelineLayers.forEachPropagated((tl, row) -> {
            g.setColor(tl.getDataColor());
            drawHorizontalLabels(g, geometry, xAxis, row + 1, tl);
        });
    }

    private void drawYAxisLabels(Graphics2D g, GraphGeometry geometry) {
        TimelineLayers.forEachYAxis((tl, axisIndex) -> drawVerticalLabels(g, geometry, tl, axisIndex, tl.getYAxis().isHighlighted()));
    }

    private static int drawString(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x, y);
        return (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
    }

    private static void drawHorizontalLabels(Graphics2D g, GraphGeometry geometry, TimeAxis xAxis, int row, TimelineLayer tl) {
        Rectangle graphArea = geometry.area();
        String tickText = TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT, xAxis.start());
        Rectangle2D tickTextBounds = g.getFontMetrics().getStringBounds(tickText, g);
        int tickTextWidth = (int) tickTextBounds.getWidth();
        int tickTextHeight = (int) tickTextBounds.getHeight() + row * DrawConstants.GRAPH_BOTTOM_AXIS_SPACE;
        int horizontalTickCount = Math.max(2, (graphArea.width - tickTextWidth * 2) / tickTextWidth);
        TimeAxis.Mapper xMapper = xMapper(geometry, xAxis, tl);
        long tickDifferenceHorizontal = (xAxis.end() - xAxis.start()) / (horizontalTickCount - 1);

        long previousDate = Long.MIN_VALUE;
        for (int i = 0; i < horizontalTickCount; ++i) {
            long tickValue = xAxis.start() + i * tickDifferenceHorizontal;
            if (tl != null) {
                tickValue = tl.getObservationTime(tickValue);
            }

            int x = xMapper.toPixel(tickValue);
            tickText = formatTickText(tickValue, previousDate);
            drawTimeTick(g, geometry, graphArea, x, tickText, tickTextHeight, tl);
            previousDate = tickValue;
        }
    }

    private static TimeAxis.Mapper xMapper(GraphGeometry geometry, TimeAxis xAxis, TimelineLayer tl) {
        if (tl == null) {
            return geometry.xMapper(xAxis);
        }
        return new TimeAxis.Mapper(tl.getObservationTime(xAxis.start()), tl.getObservationTime(xAxis.end()), geometry.area().x, geometry.area().width);
    }

    private static String formatTickText(long tickValue, long previousDate) {
        if (previousDate == Long.MIN_VALUE) {
            return TimeUtils.format(DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
        }

        long tickDayNumber = tickValue / TimeUtils.DAY_IN_MILLIS;
        long prevDayNumber = previousDate / TimeUtils.DAY_IN_MILLIS;
        return TimeUtils.format(tickDayNumber == prevDayNumber ? DrawConstants.HOUR_TIME_FORMAT : DrawConstants.FULL_DATE_TIME_FORMAT_REVERSE, tickValue);
    }

    private static void drawTimeTick(Graphics2D g, GraphGeometry geometry, Rectangle graphArea, int x, String tickText, int tickTextHeight, TimelineLayer tl) {
        if (tl == null) {
            g.setColor(UIGlobals.TL_TICK_LINE_COLOR);
            g.drawLine(x, graphArea.y, x, geometry.graphBottom() + 3);
            g.setColor(UIGlobals.TL_LABEL_TEXT_COLOR);
        } else {
            g.setColor(tl.getDataColor());
        }

        int y = geometry.graphBottom() + 2 + tickTextHeight;
        for (String line : Regex.Return.split(tickText)) {
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
        YAxis.Ticks ticks = yAxis.ticks(yMapper, geometry.area().y, geometry.graphBottom());

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

        if (highlight) {
            Stroke s = g.getStroke();
            g.setStroke(boldStroke);
            g.setFont(DrawConstants.fontBold);
            g.drawLine(axisX, graphArea.y, axisX, graphArea.y + graphArea.height + 3);
            g.drawString(verticalLabel, axisX - labelCompensation, vHeight);
            g.setStroke(s);
            g.setFont(DrawConstants.font);
        } else {
            g.drawLine(axisX, graphArea.y, axisX, graphArea.y + graphArea.height + 3);
            g.drawString(verticalLabel, axisX - labelCompensation, vHeight);
        }
    }

    private static void drawHorizontalTickline(Graphics g, Rectangle graphArea, YAxis.Mapper yMapper, double tick, int axisX, int leftSide, boolean needTxt, boolean highlight) {
        String tickText = DrawConstants.valueFormatter.format(tick);
        int y = yMapper.scaledToPixel(tick);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(tickText, g);
        int xText;
        if (leftSide == -1) {
            xText = axisX - 6 - (int) bounds.getWidth();
            g.drawLine(axisX - 3, y, graphArea.x + graphArea.width, y);
        } else {
            xText = axisX;
        }
        if (needTxt) {
            if (highlight) {
                g.setFont(DrawConstants.fontBold);
                g.drawString(tickText, xText, y + (int) (bounds.getHeight() / 2));
                g.setFont(DrawConstants.font);
            } else {
                g.drawString(tickText, xText, y + (int) (bounds.getHeight() / 2));
            }
        }
    }

}
