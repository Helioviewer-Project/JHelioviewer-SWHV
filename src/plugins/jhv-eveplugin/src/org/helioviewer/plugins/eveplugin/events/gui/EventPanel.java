package org.helioviewer.plugins.eveplugin.events.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.events.model.EventModel;
import org.helioviewer.plugins.eveplugin.events.model.EventPlotConfiguration;
import org.helioviewer.plugins.eveplugin.events.model.EventTypePlotConfiguration;

public class EventPanel implements DrawableElement {

    private final YAxisElement yAxisElement;

    public EventPanel() {
        yAxisElement = new YAxisElement(new Range(0, 0), new Range(0, 0), "", 0, 0, Color.BLACK, false, System.currentTimeMillis());
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.EVENT;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxis, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            EventTypePlotConfiguration etpc = EventModel.getSingletonInstance().getEventTypePlotConfiguration();
            Map<String, List<EventPlotConfiguration>> epcs = etpc.getEventPlotConfigurations();
            int eventTypeNr = 0;
            int previousLine = 0;
            float dash1[] = { 10.0f };
            BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
            Stroke normalStroke = g.getStroke();
            for (String eventType : epcs.keySet()) {
                boolean first = true;
                int spacePerLine = 0;
                for (EventPlotConfiguration epc : epcs.get(eventType)) {
                    epc.draw(g, graphArea, etpc.getNrOfEventTypes(), eventTypeNr, etpc.getMaxLinesPerEventType().get(eventType).intValue(), etpc.getTotalNrLines(), previousLine, mousePosition);
                    if (first) {
                        spacePerLine = 2 * Math.min(4, (new Double(Math.floor(1.0 * graphArea.height / etpc.getTotalNrLines() / 2))).intValue());
                        int spaceNeeded = spacePerLine * etpc.getMaxLinesPerEventType().get(eventType).intValue();
                        ImageIcon icon = epc.getEvent().getIcon();
                        leftAxis.drawImage(icon.getImage(), 0, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 - icon.getIconHeight() / 2 / 2, icon.getIconWidth() / 2, leftAxisArea.y + previousLine * spacePerLine + spaceNeeded / 2 + icon.getIconHeight() / 2 / 2, 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
                    }
                    first = false;
                }
                previousLine += etpc.getMaxLinesPerEventType().get(eventType).intValue();
                if (eventTypeNr != epcs.size() - 1) {
                    g.setStroke(dashed);
                    g.setColor(Color.black);
                    int sepLinePos = previousLine * spacePerLine - spacePerLine / 2;
                    g.drawLine(0, sepLinePos, graphArea.width, sepLinePos);
                    g.setStroke(normalStroke);
                }
                eventTypeNr++;
            }
        }
    }

    @Override
    public void setYAxisElement(YAxisElement yAxisElement) {
        // TODO Auto-generated method stub

    }

    @Override
    public YAxisElement getYAxisElement() {
        // return yAxisElement;
        return null;
    }

    @Override
    public boolean hasElementsToDraw() {
        return EventModel.getSingletonInstance().hasElementsToDraw();
    }

    @Override
    public Date getLastDateWithData() {
        return EventModel.getSingletonInstance().getEventTypePlotConfiguration().getLastDateWithData();
    }
}
