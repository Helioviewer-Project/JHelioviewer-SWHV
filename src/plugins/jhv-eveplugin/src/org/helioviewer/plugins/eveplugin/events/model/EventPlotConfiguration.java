package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.helioviewer.jhv.data.datatype.JHVEvent;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventPlotConfiguration {
    /** The event */
    private final JHVEvent event;

    /** The scaled x position */
    private final double scaledX0;
    private final double scaledX1;

    /**
     * Creates a EventPlotConfiguration for the given event with scaledX0 start
     * position and scaledX1 end position.
     * 
     * @param event
     *            the event for this plot configuration
     * @param scaledX0
     *            the scaled start position
     * @param scaledX1
     *            the scaled end position
     */
    public EventPlotConfiguration(JHVEvent event, double scaledX0, double scaledX1) {
        this.event = event;
        this.scaledX0 = scaledX0;
        this.scaledX1 = scaledX1;
    }

    /**
     * Draws the event plot configuration on the given graph area.
     * 
     * @param g
     *            the graphics on which to draw
     * @param graphArea
     *            the area available to draw
     */
    public void draw(Graphics g, Rectangle graphArea) {
        g.setColor(Color.CYAN);
        g.fillRect((new Double(Math.floor(graphArea.width * scaledX0))).intValue(), 10,
                (new Double(Math.floor(graphArea.width * (scaledX1 - scaledX0)))).intValue(), 2);
        // g.drawString(event.getDisplayName(), (new
        // Double(Math.floor(graphArea.width * scaledX0))).intValue(), 60);
        g.drawImage(event.getIcon().getImage(), (new Double(Math.floor(graphArea.width * scaledX0))).intValue(), 60, null);
    }
}
