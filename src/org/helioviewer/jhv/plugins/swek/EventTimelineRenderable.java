package org.helioviewer.jhv.plugins.swek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.cache.JHVEventHandler;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.data.cache.SortedDateInterval;
import org.helioviewer.jhv.data.event.JHVEventParameter;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawConstants;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.view.linedataselector.AbstractTimelineRenderable;
import org.json.JSONObject;

public class EventTimelineRenderable extends AbstractTimelineRenderable implements JHVEventHandler {

    private final YAxis yAxis = new YAxis(0, 0, "Events", false);
    private static EventPlotConfiguration eventUnderMouse;
    private static JHVRelatedEvents highlightedEvent = null;
    private static int highlightedEventPosition = -1;

    public EventTimelineRenderable() {
    }

    public EventTimelineRenderable(JSONObject jo) {
    }

    @Override
    public void serialize(JSONObject jo) {
    }

    @Override
    public void fetchData(TimeAxis selectedAxis) {
        JHVEventCache.requestForInterval(selectedAxis.start - TimeUtils.DAY_IN_MILLIS * 3, selectedAxis.end, this);
    }

    @Override
    public void newEventsReceived() {
        if (enabled)
            DrawController.drawRequest();
    }

    @Override
    public void cacheUpdated() {
        TimeAxis xAxis = DrawController.selectedAxis;
        JHVEventCache.requestForInterval(xAxis.start, xAxis.end, this);
        if (enabled)
            DrawController.drawRequest();
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (!enabled)
            return;

        highlightedEvent = null;
        highlightedEventPosition = -1;

        Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = JHVEventCache.get(xAxis.start, xAxis.end, xAxis.start, xAxis.end).getAvailableEvents();
        if (events.isEmpty())
            return;

        EventPlotConfiguration shouldRedraw = null;

        ArrayList<Long> endDates = new ArrayList<>();
        int nrLines = 0;
        for (SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap : events.values()) {
            for (JHVRelatedEvents event : eventMap.values()) {
                int i = 0;
                while (i < nrLines && endDates.get(i) >= event.getStart()) {
                    i++;
                }
                if (i == nrLines) {
                    endDates.add(event.getEnd());
                } else {
                    endDates.set(i, event.getEnd());
                }
                int eventPosition = i;
                nrLines = Math.max(nrLines, endDates.size());

                int x0 = xAxis.value2pixel(graphArea.x, graphArea.width, event.getStart());
                int x1 = xAxis.value2pixel(graphArea.x, graphArea.width, event.getEnd());
                JHVRelatedEvents rEvent = EventPlotConfiguration.draw(graphArea, event, x0, x1, eventPosition, g, mousePosition, event.isHighlighted());
                if (rEvent != null) {
                    shouldRedraw = new EventPlotConfiguration(rEvent, x0, x1, eventPosition);
                    highlightedEvent = rEvent;
                    highlightedEventPosition = eventPosition;
                }
            }
        }
        eventUnderMouse = shouldRedraw;
    }

    @Override
    public void drawHighlighted(Graphics2D g, Rectangle graphArea, TimeAxis xAxis, Point mousePosition) {
        if (mousePosition != null) {
            if (highlightedEvent != null) {
                int x0 = xAxis.value2pixel(graphArea.x, graphArea.width, highlightedEvent.getStart());
                int x1 = xAxis.value2pixel(graphArea.x, graphArea.width, highlightedEvent.getEnd());
                EventPlotConfiguration.draw(graphArea, highlightedEvent, x0, x1, highlightedEventPosition, g, mousePosition, highlightedEvent.isHighlighted());
            }
            JHVEventCache.highlight(highlightedEvent);
        }
    }

    @Override
    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public void remove() {
    }

    @Override
    public String getName() {
        return "SWEK Events";
    }

    @Override
    public Color getDataColor() {
        return null;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public Component getOptionsPanel() {
        return null;
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
    public boolean showYAxis() {
        return false;
    }

    private static class EventPlotConfiguration {

        final JHVRelatedEvents event;

        final int x0;
        final int x1;
        final int yPosition;

        EventPlotConfiguration(JHVRelatedEvents _event, int _x0, int _x1, int _yPosition) {
            event = _event;
            x0 = _x0;
            x1 = _x1;
            yPosition = _yPosition;
        }

        static JHVRelatedEvents draw(Rectangle graphArea, JHVRelatedEvents event, int x0, int x1, int yPosition, Graphics2D g, Point mousePosition, boolean highlight) {
            int spacePerLine = 3;
            int y = graphArea.y + spacePerLine * 2 * yPosition + DrawConstants.EVENT_OFFSET;
            int w = Math.max(x1 - x0, 1);
            int h = spacePerLine;
            if (w < 5) {
                x0 -= 5 / w;
                w = 5;
            }

            boolean containsMouse = containsPoint(mousePosition, x0 - 1, y - 1, w + 2, h + 2);
            boolean eventWasHightlighted = containsMouse || (mousePosition == null && event.isHighlighted());
            boolean hl = eventWasHightlighted && highlight;
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

            ImageIcon icon = event.getIcon();
            g.drawImage(icon.getImage(), x0 + w / 2 - sz / 2, y + h / 2 - sz / 2, x0 + w / 2 + sz / 2, y + h / 2 + sz / 2, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);

            if (hl) {
                drawText(graphArea, g, event, y, mousePosition);
            }

            return containsMouse ? highlightedEvent = event : null;
        }

        private static void drawText(Rectangle graphArea, Graphics2D g, JHVRelatedEvents event, int y, Point mousePosition) {
            if (mousePosition != null) {
                long ts = DrawController.selectedAxis.pixel2value(graphArea.x, graphArea.width, mousePosition.x);
                ArrayList<String> txts = new ArrayList<>();
                JHVEventParameter[] params = event.getClosestTo(ts).getSimpleVisibleEventParameters();
                int width = 1;
                for (JHVEventParameter p : params) {
                    String name = p.getParameterName();
                    if (name != "event_description" && name != "event_title") { // interned
                        String str = p.getParameterDisplayName() + " : " + p.getSimpleDisplayParameterValue();
                        txts.add(str);
                        width = Math.max(width, g.getFontMetrics().stringWidth(str));
                    }
                }
                g.setColor(DrawConstants.TEXT_BACKGROUND_COLOR);
                g.fillRect(mousePosition.x + 5, y, width + 21 + 10, (txts.size()) * 10 + 11);
                g.setColor(DrawConstants.TEXT_COLOR);
                y += 5;
                ImageIcon icon = event.getIcon();
                g.drawImage(icon.getImage(), mousePosition.x + 8, y - 2, mousePosition.x + 24, y + 14, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
                for (String txt : txts) {
                    g.drawString(txt, mousePosition.x + 26, y += 10);
                }
            }
        }

        private static boolean containsPoint(Point p, int clickx, int clicky, int clickw, int clickh) {
            return p != null && p.x >= clickx && p.x <= clickx + clickw && p.y >= clicky && p.y <= clicky + clickh;
        }
    }

    @Override
    public void zoomToFitAxis() {
    }

    @Override
    public void resetAxis() {
    }

    @Override
    public boolean highLightChanged(Point p) {
        if (!enabled)
            return false;
        if (eventUnderMouse == null)
            return true;
        return !(eventUnderMouse.x0 <= p.x && p.x <= eventUnderMouse.x1 && eventUnderMouse.yPosition - 4 <= p.y && p.y <= eventUnderMouse.yPosition + 5);
    }

    @Override
    public ClickableDrawable getDrawableUnderMouse() {
        return eventUnderMouse == null ? null : eventUnderMouse.event;
    }

}
