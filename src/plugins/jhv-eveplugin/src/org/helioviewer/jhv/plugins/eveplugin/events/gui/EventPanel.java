package org.helioviewer.jhv.plugins.eveplugin.events.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
            // EventTypePlotConfiguration etpc =
            // EventModel.getSingletonInstance().getEventTypePlotConfiguration();

            int nrEventTypes = events.size();
            int eventTypeNr = 0;
            int previousLine = 0;

            BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0f);
            Stroke normalStroke = g.getStroke();
            JHVRelatedEvents highlightedEvent = null;

            for (JHVEventType eventType : events.keySet()) {
                // int maxLines =
                // etpc.getMaxLinesPerEventType().get(eventType).intValue();
                ArrayList<Date> endDates = new ArrayList<Date>();
                List<EventPlotConfiguration> plotConfig = new ArrayList<EventPlotConfiguration>();
                Date minimalEndDate = null;
                Date maximumEndDate = null;
                int minimalDateLine = 0;
                int maximumDateLine = 0;
                int nrLines = 0;
                int maxEventLines = 0;
                SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap = events.get(eventType);
                int spacePerLine = 0;
                EventPlotConfiguration shouldRedraw = null;
                ImageIcon icon = null;
                for (Entry<SortedDateInterval, JHVRelatedEvents> evr : eventMap.entrySet()) {
                    JHVRelatedEvents event = evr.getValue();
                    icon = event.getIcon();
                    int eventPosition = 0;
                    if (minimalEndDate == null || minimalEndDate.getTime() >= event.getStart()) {
                        minimalEndDate = new Date(event.getEnd());
                        endDates.add(minimalEndDate);
                        eventPosition = nrLines;
                        nrLines++;
                    } else {
                        if (event.getStart() > maximumEndDate.getTime()) {
                            eventPosition = 0;
                            nrLines = 1;
                            endDates = new ArrayList<Date>();
                            endDates.add(new Date(event.getEnd()));
                        } else {
                            eventPosition = minimalDateLine;
                            endDates.set(minimalDateLine, new Date(event.getEnd()));
                        }
                    }
                    minimalDateLine = defineMinimalDateLine(endDates);
                    minimalEndDate = endDates.get(minimalDateLine);
                    maximumDateLine = defineMaximumDateLine(endDates);
                    maximumEndDate = endDates.get(maximumDateLine);
                    double scaledX0 = defineScaledValue(event.getStart(), selectedInterval);
                    double scaledX1 = defineScaledValue(event.getEnd(), selectedInterval);
                    if (nrLines > maxEventLines) {
                        maxEventLines = nrLines;
                    }
                    EventPlotConfiguration epc = new EventPlotConfiguration(event, scaledX0, scaledX1, eventPosition);
                    JHVRelatedEvents rEvent = epc.draw(g, graphArea, nrEventTypes, eventTypeNr, previousLine, mousePosition);
                    if (rEvent != null) {
                        highlightedEvent = rEvent;
                    }

                    if (epc.shouldRedraw()) {
                        shouldRedraw = epc;
                    }
                }
                if (shouldRedraw != null) {
                    shouldRedraw.draw(g, graphArea, nrEventTypes, eventTypeNr, previousLine, mousePosition);
                }

                if (icon != null) {
                    spacePerLine = 6;
                    int spaceNeeded = spacePerLine * maxEventLines;
                    leftAxis.drawImage(icon.getImage(), 0, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 - icon.getIconHeight() / 2 / 2, icon.getIconWidth() / 2, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 + icon.getIconHeight() / 2 / 2, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
                }

                previousLine += maxEventLines;
                if (eventTypeNr != nrEventTypes - 1) {
                    g.setStroke(dashed);
                    g.setColor(Color.black);
                    int sepLinePos = previousLine * spacePerLine - spacePerLine / 4 + DrawConstants.EVENT_OFFSET;
                    g.drawLine(0, sepLinePos, graphArea.width, sepLinePos);
                    g.setStroke(normalStroke);
                }

                eventTypeNr++;
            }
            if (mousePosition != null) {
                JHVEventContainer.highlight(highlightedEvent);
            }
        }
    }

    private int defineMaximumDateLine(ArrayList<Date> endDates) {
        Date maxDate = null;
        int maxLine = 0;
        for (Date d : endDates) {
            if (maxDate == null) {
                // first case
                maxDate = d;
                maxLine = 0;
            } else {
                // the rest
                if (d.after(maxDate)) {
                    maxDate = d;
                    maxLine = endDates.indexOf(d);
                }
            }
        }
        return maxLine;
    }

    private int defineMinimalDateLine(ArrayList<Date> endDates) {
        Date minDate = null;
        int minLine = 0;
        for (Date d : endDates) {
            if (minDate == null) {
                // first case
                minDate = d;
                minLine = 0;
            } else {
                // the rest
                if (d.before(minDate)) {
                    minDate = d;
                    minLine = endDates.indexOf(d);
                }
            }
        }
        return minLine;
    }

    private double defineScaledValue(long date, Interval<Date> selectedInterval) {
        double selectedDuration = 1.0 * (selectedInterval.getEnd().getTime() - selectedInterval.getStart().getTime());
        double position = 1.0 * (date - selectedInterval.getStart().getTime());
        return position / selectedDuration;
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
