package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.helioviewer.jhv.data.datatype.JHVEvent;

public class EventPlotConfiguration {
    private final JHVEvent event;
    private final double scaledX0;
    private final double scaledX1;

    public EventPlotConfiguration(JHVEvent event, double scaledX0, double scaledX1) {
        this.event = event;
        this.scaledX0 = scaledX0;
        this.scaledX1 = scaledX1;
    }

    public void draw(Graphics g, Rectangle graphArea) {
        g.setColor(Color.CYAN);
        g.fillRect((new Double(Math.floor(graphArea.width * scaledX0))).intValue(), 10,
                (new Double(Math.floor(graphArea.width * (scaledX1 - scaledX0)))).intValue(), 2);
        g.drawString(event.getDisplayName(), (new Double(Math.floor(graphArea.width * scaledX0))).intValue(), 60);
    }
}
