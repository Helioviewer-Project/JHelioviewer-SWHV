package org.helioviewer.jhv.plugins.eveplugin.events.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventPlotConfiguration;

public class EventPanel implements DrawableElement {

    private static final float dash1[] = { 10f };

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.EVENT;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxis, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = EventModel.getSingletonInstance().getEvents();
        if (EventModel.getSingletonInstance().isEventsVisible() && events.size() > 0) {
            final Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
            long selectedIntervalStart = selectedInterval.getStart().getTime();
            long selectedIntervalEnd = selectedInterval.getEnd().getTime();

            int nrEventTypes = events.size();
            int eventTypeNr = 0;
            int previousLine = 0;

            BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0f);
            Stroke normalStroke = g.getStroke();
            JHVRelatedEvents highlightedEvent = null;
            int spacePerLine = 6;

            for (JHVEventType eventType : events.keySet()) {

                ArrayList<Long> endDates = new ArrayList<Long>();
                int nrLines = 0;
                EventPlotConfiguration shouldRedraw = null;

                SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap = events.get(eventType);
                for (Entry<SortedDateInterval, JHVRelatedEvents> evr : eventMap.entrySet()) {
                    JHVRelatedEvents event = evr.getValue();
                    int i = 0;
                    while (i < nrLines && endDates.get(i) >= event.getStart()) {
                        i++;
                    }
                    if (i == nrLines)
                        endDates.add(event.getEnd());
                    else
                        endDates.set(i, event.getEnd());
                    int eventPosition = i;
                    nrLines = endDates.size();

                    int x0 = (int) (graphArea.width * defineScaledValue(event.getStart(), selectedIntervalStart, selectedIntervalEnd));
                    int x1 = (int) (graphArea.width * defineScaledValue(event.getEnd(), selectedIntervalStart, selectedIntervalEnd));
                    JHVRelatedEvents rEvent = EventPlotConfiguration.draw(event, x0, x1, eventPosition, g, previousLine, mousePosition);
                    if (rEvent != null) {
                        shouldRedraw = new EventPlotConfiguration(event, x0, x1, eventPosition);
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

    private double defineScaledValue(long date, long start, long end) {
        return (1.0 * (date - start)) / (end - start);
    }

    @Override
    public void setYAxisElement(YAxisElement yAxisElement) {
    }

    @Override
    public YAxisElement getYAxisElement() {
        return null;
    }

    @Override
    public boolean hasElementsToDraw() {
        return EventModel.getSingletonInstance().hasElementsToDraw();
    }

    @Override
    public Date getLastDateWithData() {
        return EventModel.getSingletonInstance().getLastDateWithData();
    }

}
