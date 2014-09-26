package org.helioviewer.plugins.eveplugin.events.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
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
        yAxisElement = new YAxisElement(new Range(0, 0), new Range(0, 0), "", 0, 0, Color.BLACK);
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.EVENT;
    }

    @Override
    public void draw(Graphics g, Rectangle graphArea) {
        long start = System.currentTimeMillis();
        Thread.dumpStack();
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            EventTypePlotConfiguration etpc = EventModel.getSingletonInstance().getEventTypePlotConfiguration();
            Map<String, List<EventPlotConfiguration>> epcs = etpc.getEventPlotConfigurations();
            int eventTypeNr = 0;
            int previousLine = 0;
            for (String eventType : epcs.keySet()) {
                for (EventPlotConfiguration epc : epcs.get(eventType)) {
                    epc.draw(g, graphArea, etpc.getNrOfEventTypes(), eventTypeNr, etpc.getMaxLinesPerEventType().get(eventType).intValue(),
                            etpc.getTotalNrLines(), previousLine);
                }
                eventTypeNr++;
                previousLine += etpc.getMaxLinesPerEventType().get(eventType).intValue();
            }
        }
        Log.info("Run draw time: " + (System.currentTimeMillis() - start));
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
        return !EventModel.getSingletonInstance().getEventTypePlotConfiguration().getEventPlotConfigurations().isEmpty();
    }
}
