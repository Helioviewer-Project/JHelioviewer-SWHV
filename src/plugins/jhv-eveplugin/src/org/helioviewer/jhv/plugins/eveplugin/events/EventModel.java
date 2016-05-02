package org.helioviewer.jhv.plugins.eveplugin.events;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class EventModel implements JHVEventHandler, LineDataSelectorElement {

    private static EventModel instance;
    private final JHVEventContainer eventContainer;
    private boolean isVisible = true;
    private Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events;
    private static final float dash1[] = { 10f };
    private static final BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0f);

    private JHVRelatedEvents eventUnderMouse;

    private EventModel() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        EVEPlugin.ldsm.addLineData(this);
    }

    public static EventModel getSingletonInstance() {
        if (instance == null) {
            instance = new EventModel();
        }
        return instance;
    }

    @Override
    public void fetchData(TimeAxis selectedAxis, TimeAxis availableAxis) {
        eventContainer.requestForInterval(availableAxis.start, availableAxis.end, EventModel.this);
    }

    @Override
    public void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events) {
        this.events = events;
        if (isVisible) {
            EVEPlugin.dc.fireRedrawRequest();
        }
    }

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> getEvents() {
        return events;
    }

    @Override
    public void cacheUpdated() {
        TimeAxis xAxis = EVEPlugin.dc.selectedAxis;
        eventContainer.requestForInterval(xAxis.start, xAxis.end, this);
        EVEPlugin.dc.fireRedrawRequest();
    }

    public JHVRelatedEvents getEventUnderMouse() {
        return eventUnderMouse;
    }

    public void setEventUnderMouse(JHVRelatedEvents event) {
        eventUnderMouse = event;
    }

    @Override
    public void draw(Graphics2D g, Rectangle graphArea, Rectangle leftAxisArea, TimeAxis timeAxis, Point mousePosition) {
        if (!isVisible) {
            return;
        }

        int nrEventTypes = events.size();
        if (nrEventTypes > 0) {
            int eventTypeNr = 0;
            int previousLine = 0;

            Stroke normalStroke = g.getStroke();
            JHVRelatedEvents highlightedEvent = null;
            int spacePerLine = 6;

            ArrayList<Long> endDates = new ArrayList<Long>();

            for (Map.Entry<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> entry : events.entrySet()) {
                JHVEventType eventType = entry.getKey();
                SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap = entry.getValue();

                endDates.clear();

                int nrLines = 0;
                EventPlotConfiguration shouldRedraw = null;

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
                    nrLines = endDates.size();

                    int x0 = timeAxis.value2pixel(graphArea.x, graphArea.width, event.getStart());
                    int x1 = timeAxis.value2pixel(graphArea.x, graphArea.width, event.getEnd());
                    JHVRelatedEvents rEvent = EventPlotConfiguration.draw(graphArea, event, x0, x1, eventPosition, g, previousLine, mousePosition, event.isHighlighted());
                    if (rEvent != null) {
                        shouldRedraw = new EventPlotConfiguration(rEvent, x0, x1, eventPosition);
                        highlightedEvent = rEvent;
                    }
                }

                if (shouldRedraw != null) {
                    shouldRedraw.draw(graphArea, g, previousLine, mousePosition);
                }

                int spaceNeeded = spacePerLine * nrLines;
                ImageIcon icon = eventType.getEventType().getEventIcon();
                g.drawImage(icon.getImage(), 0, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 - icon.getIconHeight() / 2 / 2, icon.getIconWidth() / 2, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 + icon.getIconHeight() / 2 / 2, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);

                previousLine += nrLines;
                if (eventTypeNr != nrEventTypes - 1) {
                    g.setStroke(dashed);
                    g.setColor(Color.black);
                    int sepLinePos = previousLine * spacePerLine - spacePerLine / 4 + DrawConstants.EVENT_OFFSET;
                    g.drawLine(0, sepLinePos, graphArea.width, sepLinePos);
                    g.setStroke(normalStroke);
                }

                eventTypeNr++;
            }
            setEventUnderMouse(highlightedEvent);
            if (mousePosition != null) {
                JHVEventContainer.highlight(highlightedEvent);
            }
        }
    }

    @Override
    public void setYAxis(YAxis yAxis) {
    }

    @Override
    public YAxis getYAxis() {
        return null;
    }

    @Override
    public void removeLineData() {
        isVisible = false;
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        EVEPlugin.dc.fireRedrawRequest();
        EVEPlugin.ldsm.lineDataElementUpdated(this);
    }

    @Override
    public boolean isVisible() {
        return isVisible;
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

    @Override
    public void yaxisChanged() {
    }

    private static class EventPlotConfiguration {
        private final JHVRelatedEvents event;

        private final int x0;
        private final int x1;

        private final int yPosition;

        public EventPlotConfiguration(JHVRelatedEvents event, int x0, int x1, int yPosition) {
            this.event = event;
            this.x0 = x0;
            this.x1 = x1;
            this.yPosition = yPosition;
        }

        public JHVRelatedEvents draw(Rectangle graphArea, Graphics2D g, int nrPreviousLines, Point mousePosition) {
            return draw(graphArea, event, x0, x1, yPosition, g, nrPreviousLines, mousePosition, true);
        }

        public static JHVRelatedEvents draw(Rectangle graphArea, JHVRelatedEvents event, int x0, int x1, int yPosition, Graphics2D g, int nrPreviousLines, Point mousePosition, boolean highlight) {
            JHVRelatedEvents highlightedEvent = null;
            int spacePerLine = 3;
            int y = graphArea.y + spacePerLine * 2 * (nrPreviousLines + yPosition) + DrawConstants.EVENT_OFFSET;
            int w = Math.max(x1 - x0, 1);
            int h = spacePerLine;
            if (w < 5) {
                x0 = x0 - (5 / w);
                w = 5;
            }

            boolean containsMouse = containsPoint(mousePosition, x0 - 1, y - 1, w + 2, h + 2);
            boolean eventWasHightlighted = containsMouse || event.isHighlighted();
            if (mousePosition != null && containsMouse) {
                highlightedEvent = event;
            }

            if (eventWasHightlighted && highlight) {
                x0 = x0 - 10;
                y = y - 1;
                w = w + 20;
                h = h + 2;
                spacePerLine = h;
            }

            g.setColor(event.getColor());
            g.fillRect(x0, y, w, spacePerLine);

            return highlightedEvent;
        }

        private static boolean containsPoint(Point p, int clickx, int clicky, int clickw, int clickh) {
            if (p != null) {
                return p.x >= clickx && p.x <= clickx + clickw && p.y >= clicky && p.y <= clicky + clickh;
            }
            return false;
        }

    }

    @Override
    public void zoomToFitAxis() {
    }

    @Override
    public void resetAxis() {
    }

}
