package org.helioviewer.jhv.plugins.swek;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

import org.json.JSONObject;

// has to be public for state
public final class EventTimelineLayer extends TimelineLayer implements SWEKSession.Listener {

    private final SWEKSession session;
    private SWEKDialogs dialogs;
    private EventPlotConfiguration eventUnderMouse;
    private final List<EventPlotConfiguration> eventPlots = new ArrayList<>();
    private List<RelatedEvents> visibleEvents = Collections.emptyList();

    EventTimelineLayer(SWEKSession _session) {
        session = _session;
        registerAndRefresh();
    }

    void setDialogs(SWEKDialogs _dialogs) {
        dialogs = _dialogs;
    }

    @Nullable
    public static TimelineLayer deserialize(JSONObject ignore) { // has to be implemented for state
        return SWEKPlugin.getTimelineLayer();
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
            registerAndRefresh();
        } else {
            session.removeListener(this);
        }
    }

    private void registerAndRefresh() {
        session.addListener(this);
        cacheUpdated();
    }

    @Override
    public void highlightChanged() {
        DrawController.highlightChanged();
    }

    @Override
    public void cacheUpdated() {
        if (!enabled) return;
        fetchData(DrawController.selectedAxis);
        DrawController.drawRequest();
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        eventUnderMouse = null;
        eventPlots.clear();
        List<RelatedEvents> events = visibleEvents;
        if (events.isEmpty()) {
            if (mousePosition != null) {
                session.highlight(null);
            }
            return;
        }

        ArrayList<Long> endDates = new ArrayList<>();
        TimeAxis.Mapper xMapper = xAxis.mapper(graphArea.x, graphArea.width);

        for (RelatedEvents event : events) {
            long eventStart = event.getStart();
            long eventEnd = event.getEnd();
            int i = 0;
            while (i < endDates.size() && endDates.get(i) >= eventStart) {
                i++;
            }
            if (i == endDates.size()) {
                endDates.add(eventEnd);
            } else {
                endDates.set(i, eventEnd);
            }
            int eventPosition = i;

            for (Interval interval : event.getIntervals()) {
                if (interval.end() < xAxis.start() || interval.start() > xAxis.end())
                    continue;
                int x0 = xMapper.toPixel(interval.start());
                int x1 = xMapper.toPixel(interval.end());
                long middle = interval.start() + (interval.end() - interval.start()) / 2;
                EventPlotConfiguration plot = createEventPlot(graphArea, event, x0, x1, eventPosition, middle);
                eventPlots.add(plot);
                drawEvent(graphArea, plot, g, mousePosition);
                if (plot.contains(mousePosition))
                    eventUnderMouse = plot;
            }
        }

        if (mousePosition != null) {
            if (eventUnderMouse != null) {
                drawEvent(graphArea, eventUnderMouse, g, mousePosition);
                session.highlight(eventUnderMouse.event);
            } else {
                session.highlight(null);
            }
        }
    }

    @Override
    public void remove() {
        setEnabled(false);
        visibleEvents = List.of();
        eventPlots.clear();
        eventUnderMouse = null;
        dialogs = null;
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

    private record EventPlotConfiguration(RelatedEvents event, int x, int y, int width, int height, long time) {
        boolean contains(Point point) {
            return containsPoint(point, x - 1, y - 1, width + 2, height + 2);
        }
    }

    private static EventPlotConfiguration createEventPlot(Rectangle graphArea, RelatedEvents event, int x0, int x1, int yPosition, long time) {
        int w = Math.max(x1 - x0, 1);
        if (w < 5) {
            x0 -= (5 - w) / 2; // center the widened marker on the event
            w = 5;
        }
        int y = graphArea.y + 6 * yPosition + DrawConstants.EVENT_OFFSET;
        return new EventPlotConfiguration(event, x0, y, w, 3, time);
    }

    private static void drawEvent(Rectangle graphArea, EventPlotConfiguration plot, Graphics2D g, Point mousePosition) {
        RelatedEvents event = plot.event;
        int x0 = plot.x;
        int y = plot.y;
        int w = plot.width;
        int h = plot.height;
        int spacePerLine = h;
        boolean containsMouse = plot.contains(mousePosition);
        boolean hl = event.isHighlighted() && (mousePosition == null || containsMouse); // null mousePosition from image canvas
        int sz = Math.min(w, 8);
        if (hl) {
            x0 -= 10;
            y -= 1;
            w += 20;
            h += 2;
            sz = 12;
            spacePerLine = h;
        }
        g.setColor(event.getColor());
        g.fillRect(x0, y, w, spacePerLine);

        BufferedImage icon = SWEKIconImages.getImage(event.getClosestTo(plot.time).getSupplier().group().getIconKey());
        g.drawImage(icon, x0 + w / 2 - sz / 2, y + h / 2 - sz / 2, x0 + w / 2 + sz / 2, y + h / 2 + sz / 2, 0, 0, icon.getWidth(), icon.getHeight(), null);

        if (hl && mousePosition != null) {
            drawText(graphArea, g, event, y, mousePosition.x);
        }
    }

    private static void drawText(Rectangle graphArea, Graphics2D g, RelatedEvents event, int y, int mouseX) {
        long ts = DrawController.selectedAxis.mapper(graphArea.x, graphArea.width).toValue(mouseX);
        SolarEvent closestEvent = event.getClosestTo(ts);
        List<String> txts = SWEKData.visibleParameterLines(closestEvent);
        int width = 1;
        for (String text : txts) {
            width = Math.max(width, g.getFontMetrics().stringWidth(text));
        }
        g.setColor(UIGlobals.TL_TEXT_BACKGROUND_COLOR);
        g.fillRect(mouseX + 5, y, width + 21 + 10, txts.size() * 10 + 11);
        g.setColor(UIGlobals.TL_TEXT_COLOR);

        y += 5;
        BufferedImage icon = SWEKIconImages.getImage(closestEvent.getSupplier().group().getIconKey());
        g.drawImage(icon, mouseX + 8, y - 2, mouseX + 24, y + 14, 0, 0, icon.getWidth(), icon.getHeight(), null);

        for (String txt : txts) {
            g.drawString(txt, mouseX + 26, y += 10);
        }
    }

    private static boolean containsPoint(Point p, int clickx, int clicky, int clickw, int clickh) {
        return p != null && p.x >= clickx && p.x <= clickx + clickw && p.y >= clicky && p.y <= clicky + clickh;
    }

    @Override
    public boolean highlightChanged(Point p) {
        if (!enabled)
            return false;

        EventPlotConfiguration current = null;
        for (int i = eventPlots.size() - 1; i >= 0; i--) {
            EventPlotConfiguration plot = eventPlots.get(i);
            if (plot.contains(p)) {
                current = plot;
                break;
            }
        }

        boolean changed = current != eventUnderMouse;
        eventUnderMouse = current;
        session.highlight(current == null ? null : current.event);
        // Event details depend on the time beneath the pointer.
        return changed || current != null;
    }

    @Nullable
    @Override
    public ClickableDrawable getDrawableUnderMouse() {
        if (eventUnderMouse == null || dialogs == null)
            return null;

        RelatedEvents event = eventUnderMouse.event;
        return (location, timestamp) -> {
            SWEKEventInformationDialog dialog = dialogs.createInformation(event, event.getClosestTo(timestamp));
            if (dialog == null) return;
            dialog.pack();
            dialog.setLocation(location);
            dialog.setVisible(true);
        };
    }

}
