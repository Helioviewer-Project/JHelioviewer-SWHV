package org.helioviewer.jhv.plugins.eveplugin.events.gui;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventPlotConfiguration;
import org.helioviewer.jhv.plugins.eveplugin.events.model.EventTypePlotConfiguration;

public class EventPanel implements DrawableElement {

    private static final float dash1[] = { 10f };

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.EVENT;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxis, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            EventTypePlotConfiguration etpc = EventModel.getSingletonInstance().getEventTypePlotConfiguration();
            int nrEventTypes = etpc.getNrOfEventTypes();
            int eventTypeNr = 0;
            int previousLine = 0;

            Map<JHVEventType, List<EventPlotConfiguration>> epcs = etpc.getEventPlotConfigurations();

            BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0f);
            Stroke normalStroke = g.getStroke();
            JHVRelatedEvents highlightedEvent = null;

            for (Map.Entry<JHVEventType, List<EventPlotConfiguration>> entry : epcs.entrySet()) {
                JHVEventType eventType = entry.getKey();
                // int maxLines =
                // etpc.getMaxLinesPerEventType().get(eventType).intValue();

                boolean first = true;
                int spacePerLine = 0;
                EventPlotConfiguration shouldRedraw = null;
                for (EventPlotConfiguration epc : entry.getValue()) {
                    JHVRelatedEvents rEvent = epc.draw(g, graphArea, nrEventTypes, eventTypeNr, previousLine, mousePosition);
                    if (rEvent != null) {
                        highlightedEvent = rEvent;
                    }

                    if (epc.shouldRedraw()) {
                        shouldRedraw = epc;
                    }
                    /*
                     * if (first) { spacePerLine = 6; int spaceNeeded =
                     * spacePerLine * maxLines; ImageIcon icon =
                     * epc.getEvent().getIcon();
                     * leftAxis.drawImage(icon.getImage(), 0, leftAxisArea.y +
                     * previousLine * spacePerLine + spaceNeeded / 2 -
                     * icon.getIconHeight() / 2 / 2, icon.getIconWidth() / 2,
                     * leftAxisArea.y + previousLine * spacePerLine +
                     * spaceNeeded / 2 + icon.getIconHeight() / 2 / 2, 0, 0,
                     * icon.getIconWidth(), icon.getIconHeight(), null); } ICON
                     * SHOULD BE DRAWN AFTER ALL EPC ARE DRAWN TOGETHER WITH
                     * DASHED LINE
                     */
                    first = false;
                }
                if (shouldRedraw != null) {
                    shouldRedraw.draw(g, graphArea, nrEventTypes, eventTypeNr, previousLine, mousePosition);
                }
                /*
                 * previousLine += maxLines; if (eventTypeNr != epcs.size() - 1)
                 * { g.setStroke(dashed); g.setColor(Color.black); int
                 * sepLinePos = previousLine * spacePerLine - spacePerLine / 4 +
                 * DrawConstants.EVENT_OFFSET; g.drawLine(0, sepLinePos,
                 * graphArea.width, sepLinePos); g.setStroke(normalStroke); }
                 */
                eventTypeNr++;
            }
            if (mousePosition != null) {
                JHVEventContainer.highlight(highlightedEvent);
            }
        }
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
