package org.helioviewer.jhv.plugins.swek;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

import org.json.JSONObject;

public final class EventTimelineLayer extends TimelineLayer implements SWEKSession.Listener {

    // Toolkit part of a click on an event: the event information window.
    public interface Ui {
        void open(RelatedEvents related, SolarEvent event, Point locationOnScreen);
    }

    // Rows are as tall as a marker icon; the first row leaves room for a highlighted icon
    private static final int ROW_HEIGHT = SWEKIconImages.MARKER;
    private static final int TOP = SWEKIconImages.MARKER_HIGHLIGHTED / 2;
    // Parameter box padding, and its distance from the pointer
    private static final int PADDING = 3;

    // An event interval: its center and width in pixels, and the center of its row
    private record Plot(RelatedEvents event, String iconKey, int center, int y, int width) {
        // A bar is at least as wide as its icon
        int barWidth(int iconSize) {
            return Math.max(width, iconSize);
        }

        // The square of the icon, widened to the bar; for a marker, the bar's row cell
        boolean contains(@Nullable Point p, int iconSize) {
            int barWidth = barWidth(iconSize);
            int left = center - barWidth / 2;
            return p != null && p.x >= left && p.x < left + barWidth && p.y >= y - iconSize / 2 && p.y < y - iconSize / 2 + iconSize;
        }
    }

    private final SWEKSession session;
    @Nullable
    private Ui ui;
    private List<RelatedEvents> visibleEvents = List.of();
    private final ArrayList<Plot> plots = new ArrayList<>();
    @Nullable
    private Plot plotUnderMouse;

    EventTimelineLayer(SWEKSession _session) {
        session = _session;
        session.addListener(this);
    }

    void setUi(@Nullable Ui _ui) {
        ui = _ui;
    }

    @Override
    public void serialize(JSONObject jo) {}

    @Override
    public void fetchData(TimeAxis selectedAxis) {
        visibleEvents = session.getEvents(selectedAxis.start(), selectedAxis.end());
        session.requestForInterval(selectedAxis.start(), selectedAxis.end());
    }

    @Override
    public void setEnabled(boolean _enabled) {
        if (enabled == _enabled) return;
        super.setEnabled(_enabled);
        if (enabled) {
            session.addListener(this);
            cacheUpdated();
        } else {
            session.removeListener(this);
        }
    }

    @Override
    public void highlightChanged() {
        DrawController.drawRequest();
    }

    @Override
    public void cacheUpdated() {
        fetchData(DrawController.selectedAxis);
        DrawController.drawRequest();
    }

    // A null mouse position means the pointer is outside the graph
    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, @Nullable Point mousePosition) {
        TimeAxis.Mapper xMapper = xAxis.mapper(graphArea.x, graphArea.width);
        plots.clear();
        // Each event goes in the first row where it does not overlap the bars already drawn; a bar reaches at most half an icon past its event
        long[] rowEnds = new long[visibleEvents.size()];
        int rows = 0;
        for (RelatedEvents event : visibleEvents) {
            long left = (long) xMapper.toPixel(event.getStart()) - SWEKIconImages.MARKER / 2;
            int row = 0;
            while (row < rows && rowEnds[row] > left)
                row++;
            if (row == rows)
                rows++;
            rowEnds[row] = (long) xMapper.toPixel(event.getEnd()) + SWEKIconImages.MARKER / 2;

            int y = graphArea.y + TOP + ROW_HEIGHT * row;
            for (Interval interval : event.getIntervals()) {
                if (interval.end() < xAxis.start() || interval.start() > xAxis.end())
                    continue;
                int x0 = xMapper.toPixel(interval.start());
                int width = xMapper.toPixel(interval.end()) - x0;
                long middle = interval.start() + (interval.end() - interval.start()) / 2;
                plots.add(new Plot(event, event.getClosestTo(middle).getSupplier().group().getIconKey(), x0 + width / 2, y, width));
            }
        }

        // The bars move with the axis, so the event beneath a still pointer can change
        plotUnderMouse = plotAt(mousePosition);
        if (mousePosition != null)
            session.highlight(plotUnderMouse == null ? null : plotUnderMouse.event());

        AffineTransform transform = g.getTransform();
        for (Plot plot : plots) {
            if (!session.isHighlighted(plot.event()))
                drawPlot(g, transform, plot, SWEKIconImages.MARKER);
        }
        for (Plot plot : plots) {
            if (session.isHighlighted(plot.event()))
                drawPlot(g, transform, plot, SWEKIconImages.MARKER_HIGHLIGHTED);
        }
        if (mousePosition != null && plotUnderMouse != null)
            drawText(g, transform, graphArea, xMapper, plotUnderMouse, mousePosition.x);
    }

    // The bar is half as thick as its icon and at least as wide; both are centered on the interval
    private static void drawPlot(Graphics2D g, AffineTransform transform, Plot plot, int iconSize) {
        int width = plot.barWidth(iconSize);
        g.setColor(plot.event().getColor());
        g.fillRect(plot.center() - width / 2, plot.y() - iconSize / 4, width, iconSize / 2);
        SWEKIconImages.draw(g, transform, plot.iconKey(), plot.center() - iconSize / 2, plot.y() - iconSize / 2, iconSize);
    }

    // The parameters of the event beneath the pointer, beside it and inside the graph; a box too large clips at the right and bottom
    private static void drawText(Graphics2D g, AffineTransform transform, Rectangle graphArea, TimeAxis.Mapper xMapper, Plot plot, int mouseX) {
        SolarEvent event = plot.event().getClosestTo(xMapper.toValue(mouseX));
        List<String> lines = SWEKData.visibleParameterLines(event);
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = 0;
        for (String line : lines)
            textWidth = Math.max(textWidth, metrics.stringWidth(line));

        int iconSize = SWEKIconImages.TEXT;
        int width = 3 * PADDING + iconSize + textWidth;
        int height = 2 * PADDING + Math.max(iconSize, lines.size() * metrics.getHeight());
        int x = mouseX + PADDING;
        if (x + width > graphArea.x + graphArea.width)
            x = Math.max(graphArea.x, mouseX - PADDING - width);
        int y = Math.max(graphArea.y, Math.min(plot.y() - SWEKIconImages.MARKER_HIGHLIGHTED / 2, graphArea.y + graphArea.height - height));

        g.setColor(UIGlobals.TL_TEXT_BACKGROUND_COLOR);
        g.fillRect(x, y, width, height);
        SWEKIconImages.draw(g, transform, event.getSupplier().group().getIconKey(), x + PADDING, y + PADDING, iconSize);

        g.setColor(UIGlobals.TL_TEXT_COLOR);
        int textX = x + 2 * PADDING + iconSize;
        int baseline = y + PADDING + metrics.getAscent();
        for (String line : lines) {
            g.drawString(line, textX, baseline);
            baseline += metrics.getHeight();
        }
    }

    // Topmost first, in reverse drawing order: highlighted plots at their drawn size, then the others
    @Nullable
    private Plot plotAt(@Nullable Point p) {
        for (int i = plots.size() - 1; i >= 0; i--) {
            Plot plot = plots.get(i);
            if (session.isHighlighted(plot.event()) && plot.contains(p, SWEKIconImages.MARKER_HIGHLIGHTED))
                return plot;
        }
        for (int i = plots.size() - 1; i >= 0; i--) {
            Plot plot = plots.get(i);
            if (plot.contains(p, SWEKIconImages.MARKER))
                return plot;
        }
        return null;
    }

    @Override
    public void remove() {
        setEnabled(false);
    }

    @Override
    public String getName() {
        return "SWEK Events";
    }

    @Nullable
    @Override
    public Color getDataColor() {
        return null;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean highlightChanged(@Nullable Point p) {
        Plot current = plotAt(p);
        boolean changed = current != plotUnderMouse;
        plotUnderMouse = current;
        session.highlight(current == null ? null : current.event());
        // Event details depend on the time beneath the pointer.
        return changed || current != null;
    }

    @Nullable
    @Override
    public ClickableDrawable getDrawableUnderMouse() {
        if (plotUnderMouse == null || ui == null)
            return null;

        Ui target = ui;
        RelatedEvents event = plotUnderMouse.event();
        return (location, timestamp) -> target.open(event, event.getClosestTo(timestamp), location);
    }

}
