package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

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

    /** the Y position */
    private final int yPosition;

    /** The position of the angle the event area */
    private Rectangle drawPosition;

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
     * @param yPosition
     *            the y-position of this event in the band provided for this
     *            event type.
     */
    public EventPlotConfiguration(JHVEvent event, double scaledX0, double scaledX1, int yPosition) {
        this.event = event;
        this.scaledX0 = scaledX0;
        this.scaledX1 = scaledX1;
        this.yPosition = yPosition;
    }

    /**
     * Draws the event plot configuration on the given graph area.
     * 
     * @param g
     *            the graphics on which to draw
     * @param graphArea
     *            the area available to draw
     * @param nrOfEventTypes
     *            the number of event types to be drawn
     * @param eventTypeNR
     *            the number of this event type
     * @param linesForEventType
     *            maximum of lines needed for this event type
     * @param totalLines
     *            the total number of lines for all events
     * @param nrPreviousLines
     *            the number of lines used already
     */
    public void draw(Graphics g, Rectangle graphArea, int nrOfEventTypes, int eventTypeNR, int linesForEventType, int totalLines,
            int nrPreviousLines) {
        int spacePerLine = Math.min(4, (new Double(Math.floor(1.0 * graphArea.height / totalLines / 2))).intValue());
        int startPosition = spacePerLine * 2 * (nrPreviousLines + yPosition);
        // g.setColor(event.getColor());
        g.setColor(event.getEventRelationShip().getRelationshipColor());
        drawPosition = new Rectangle((new Double(Math.floor(graphArea.width * scaledX0))).intValue(), startPosition, (new Double(
                Math.floor(graphArea.width * (scaledX1 - scaledX0)))).intValue(), spacePerLine);
        g.fillRect((new Double(Math.floor(graphArea.width * scaledX0))).intValue(), startPosition,
                (new Double(Math.floor(graphArea.width * (scaledX1 - scaledX0)))).intValue(), spacePerLine);
        // g.drawString(event.getDisplayName(), (new
        // Double(Math.floor(graphArea.width * scaledX0))).intValue(), 60);
        // g.drawImage(event.getIcon().getImage(), (new
        // Double(Math.floor(graphArea.width * scaledX0))).intValue(), 60,
        // null);

    }

    /**
     * Gets the event at the given point.
     * 
     * @param p
     *            the location to check for an event.
     * @return null if no event is located there, the event if found
     */
    public JHVEvent getEventAtPoint(Point p) {
        if (containsPoint(p)) {
            return event;
        }
        return null;
    }

    public JHVEvent getEvent() {
        return event;
    }

    /**
     * Checks if the given point is located where the event was drawn.
     * 
     * @param p
     *            the point to check
     * @return true if the point is located in the event area, false if the
     *         point is not located in the event area.
     */
    private boolean containsPoint(Point p) {
        if (drawPosition != null) {
            return drawPosition.contains(p);
        }
        return false;
    }

}
