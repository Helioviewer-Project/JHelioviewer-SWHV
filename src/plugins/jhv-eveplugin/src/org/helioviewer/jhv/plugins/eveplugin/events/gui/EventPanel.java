package org.helioviewer.jhv.plugins.eveplugin.events.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventPlotConfiguration;

public class EventPanel implements DrawableElement {

    private static final float dash1[] = { 10f };
    private final DrawController drawController = EVEPlugin.dc;

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.EVENT;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxis, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        if (!EventModel.getSingletonInstance().isEventsVisible()) {
            return;
        }

        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = EventModel.getSingletonInstance().getEvents();
        if (events.size() > 0) {

            int nrEventTypes = events.size();
            int eventTypeNr = 0;
            int previousLine = 0;

            BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0f);
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

                    int x0 = drawController.selectedAxis.value2pixel(graphArea.x, graphArea.width, event.getStart());
                    int x1 = drawController.selectedAxis.value2pixel(graphArea.x, graphArea.width, event.getEnd());
                    JHVRelatedEvents rEvent = EventPlotConfiguration.draw(event, x0, x1, eventPosition, g, previousLine, mousePosition, event.isHighlighted());
                    if (rEvent != null) {
                        shouldRedraw = new EventPlotConfiguration(rEvent, x0, x1, eventPosition);
                        highlightedEvent = rEvent;
                    }
                }

                if (shouldRedraw != null) {
                    shouldRedraw.draw(g, previousLine, mousePosition);
                }

                int spaceNeeded = spacePerLine * nrLines;
                ImageIcon icon = eventType.getEventType().getEventIcon();
                leftAxis.drawImage(icon.getImage(), 0, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 - icon.getIconHeight() / 2 / 2, icon.getIconWidth() / 2, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 + icon.getIconHeight() / 2 / 2, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);

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
            EventModel.getSingletonInstance().setEventUnderMouse(highlightedEvent);
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
    public boolean hasElementsToDraw() {
        return EventModel.getSingletonInstance().hasElementsToDraw();
    }

    @Override
    public long getLastDateWithData() {
        return EventModel.getSingletonInstance().getLastDateWithData();
    }

}
