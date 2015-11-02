package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Color;
import java.awt.Graphics2D;
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

    private final int offset = 3;

    /** the Y position */
    private final int yPosition;

    /** The position of the angle the event area */
    private Rectangle drawPosition;

    /** The click position */
    private Rectangle clickPosition;

    private boolean shouldRedraw;

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
        if (scaledX0 < scaledX1) {
            this.scaledX0 = scaledX0;
            this.scaledX1 = scaledX1;
        } else {
            this.scaledX0 = scaledX1;
            this.scaledX1 = scaledX0;
        }
        this.yPosition = yPosition;
        shouldRedraw = false;
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
    public JHVEvent draw(Graphics2D g, Rectangle graphArea, int nrOfEventTypes, int eventTypeNR, int linesForEventType, int totalLines, int nrPreviousLines, Point mousePosition) {
        JHVEvent highlightedEvent = null;
        int spacePerLine = Math.max(3, Math.min(4, (new Double(Math.floor(1.0 * graphArea.height / totalLines / 2))).intValue()));
        int startPosition = spacePerLine * 2 * (nrPreviousLines + yPosition) + offset;
        drawPosition = new Rectangle((int) Math.floor(graphArea.width * scaledX0), startPosition, (int) Math.floor(graphArea.width * (scaledX1 - scaledX0)) + 1, spacePerLine);
        // minimal width is 1
        if (drawPosition.width < 5) {
            drawPosition.x = drawPosition.x - (5 / drawPosition.width);
            drawPosition.width = 5;
        }

        clickPosition = new Rectangle(drawPosition.x - 1, drawPosition.y - 1, drawPosition.width + 2, drawPosition.height + 2);

        boolean containsMouse = containsPoint(mousePosition);
        boolean eventWasHightlighted = false;
        int endpointsMarkWidth = 0;
        if (containsMouse || event.isHighlighted()) {
            endpointsMarkWidth = 5;
            eventWasHightlighted = true;
            drawPosition.x = drawPosition.x - 10;
            drawPosition.y = drawPosition.y - 1;
            startPosition = startPosition - 1;
            drawPosition.width = drawPosition.width + 20;
            drawPosition.height = drawPosition.height + 2;
            shouldRedraw = true;
            spacePerLine = drawPosition.height;

        }
        if (mousePosition != null && containsMouse)
            highlightedEvent = event;
        if (containsMouse || eventWasHightlighted) {
            g.setColor(Color.black);
            g.fillRect(drawPosition.x, startPosition, endpointsMarkWidth, spacePerLine);
        }
        g.setColor(event.getEventRelationShip().getRelationshipColor());
        g.fillRect(drawPosition.x + endpointsMarkWidth, startPosition, drawPosition.width - 2 * endpointsMarkWidth, spacePerLine);
        if (containsMouse || eventWasHightlighted) {
            g.setColor(Color.black);
            g.fillRect(drawPosition.x + drawPosition.width - endpointsMarkWidth, startPosition, endpointsMarkWidth, spacePerLine);
        }
        return highlightedEvent;
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
        if (clickPosition != null && p != null) {
            return clickPosition.contains(p);
        }
        return false;
    }

    public int getEventPosition() {
        return yPosition;
    }

    public boolean shouldRedraw() {
        return shouldRedraw;
    }

}
