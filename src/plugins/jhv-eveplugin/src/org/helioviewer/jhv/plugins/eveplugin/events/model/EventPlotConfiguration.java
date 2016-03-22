package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;

/**
 *
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventPlotConfiguration {
    /** The event */
    private final JHVRelatedEvents event;

    /** The scaled x position */
    private final double scaledX0;
    private final double scaledX1;

    /** the Y position */
    private final int yPosition;

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
    public EventPlotConfiguration(JHVRelatedEvents event, double scaledX0, double scaledX1, int yPosition) {
        this.event = event;
        if (scaledX0 < scaledX1) {
            this.scaledX0 = scaledX0;
            this.scaledX1 = scaledX1;
        } else {
            this.scaledX0 = scaledX1;
            this.scaledX1 = scaledX0;
        }
        this.yPosition = yPosition;
    }

    public JHVRelatedEvents draw(Graphics2D g, Rectangle graphArea, int nrOfEventTypes, int eventTypeNR, int nrPreviousLines, Point mousePosition) {
        return draw(event, scaledX0, scaledX1, yPosition, g, graphArea, nrOfEventTypes, eventTypeNR, nrPreviousLines, mousePosition);
    }

    public static JHVRelatedEvents draw(JHVRelatedEvents event, double scaledX0, double scaledX1, int yPosition, Graphics2D g, Rectangle graphArea, int nrOfEventTypes, int eventTypeNR, int nrPreviousLines, Point mousePosition) {
        JHVRelatedEvents highlightedEvent = null;
        int spacePerLine = 3;
        int startPosition = spacePerLine * 2 * (nrPreviousLines + yPosition) + DrawConstants.EVENT_OFFSET;
        int x = (int) Math.floor(graphArea.width * scaledX0);
        int y = startPosition;
        int w = (int) Math.floor(graphArea.width * (scaledX1 - scaledX0)) + 1;
        int h = spacePerLine;
        // minimal width is 1
        if (w < 5) {
            x = x - (5 / w);
            w = 5;
        }

        boolean containsMouse = containsPoint(mousePosition, new Rectangle(x - 1, y - 1, w + 2, h + 2));
        boolean eventWasHightlighted = false;
        int endpointsMarkWidth = 0;

        if (containsMouse || event.isHighlighted()) {
            endpointsMarkWidth = 5;
            eventWasHightlighted = true;
            x = x - 10;
            y = y - 1;
            startPosition = startPosition - 1;
            w = w + 20;
            h = h + 2;
            spacePerLine = h;
        }
        if (mousePosition != null && containsMouse) {
            highlightedEvent = event;
        }
        if (containsMouse || eventWasHightlighted) {
            g.setColor(Color.black);
            g.fillRect(x, startPosition, endpointsMarkWidth, spacePerLine);
        }
        g.setColor(event.getColor());
        g.fillRect(x + endpointsMarkWidth, startPosition, w - 2 * endpointsMarkWidth, spacePerLine);
        if (containsMouse || eventWasHightlighted) {
            g.setColor(Color.black);
            g.fillRect(x + w - endpointsMarkWidth, startPosition, endpointsMarkWidth, spacePerLine);
        }

        return highlightedEvent;
    }

    public JHVRelatedEvents getEvent() {
        return event;
    }

    /**
     * Checks if the given point is located where the event was drawn.
     *
     * @param p
     *            the point to check
     * @param rectangle
     * @return true if the point is located in the event area, false if the
     *         point is not located in the event area.
     */
    private static boolean containsPoint(Point p, Rectangle clickPosition) {
        if (clickPosition != null && p != null) {
            return clickPosition.contains(p);
        }
        return false;
    }

}
