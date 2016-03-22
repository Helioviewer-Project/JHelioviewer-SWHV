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
    private final JHVRelatedEvents event;

    private final double scaledX0;
    private final double scaledX1;

    private final int yPosition;

    public EventPlotConfiguration(JHVRelatedEvents event, double scaledX0, double scaledX1, int yPosition) {
        this.event = event;
        this.scaledX0 = scaledX0;
        this.scaledX1 = scaledX1;
        this.yPosition = yPosition;
    }

    public JHVRelatedEvents draw(Graphics2D g, Rectangle graphArea, int nrPreviousLines, Point mousePosition) {
        return draw(event, scaledX0, scaledX1, yPosition, g, graphArea, nrPreviousLines, mousePosition);
    }

    public static JHVRelatedEvents draw(JHVRelatedEvents event, double scaledX0, double scaledX1, int yPosition, Graphics2D g, Rectangle graphArea, int nrPreviousLines, Point mousePosition) {
        JHVRelatedEvents highlightedEvent = null;
        int spacePerLine = 3;
        int startPosition = spacePerLine * 2 * (nrPreviousLines + yPosition) + DrawConstants.EVENT_OFFSET;
        int x = (int) Math.floor(graphArea.width * scaledX0);
        int y = startPosition;
        int w = Math.max((int) (graphArea.width * (scaledX1 - scaledX0)), 1);
        int h = spacePerLine;
        if (w < 5) {
            x = x - (5 / w);
            w = 5;
        }

        boolean containsMouse = containsPoint(mousePosition, x - 1, y - 1, w + 2, h + 2);
        boolean eventWasHightlighted = containsMouse || event.isHighlighted();
        int endpointsMarkWidth = 0;
        if (mousePosition != null && containsMouse) {
            highlightedEvent = event;
        }
        if (eventWasHightlighted) {
            endpointsMarkWidth = 5;
            x = x - 10;
            y = y - 1;
            startPosition = startPosition - 1;
            w = w + 20;
            h = h + 2;
            spacePerLine = h;

            g.setColor(Color.black);
            g.fillRect(x, startPosition, endpointsMarkWidth, spacePerLine);
            g.fillRect(x + w - endpointsMarkWidth, startPosition, endpointsMarkWidth, spacePerLine);
        }
        g.setColor(event.getColor());
        g.fillRect(x + endpointsMarkWidth, startPosition, w - 2 * endpointsMarkWidth, spacePerLine);

        return highlightedEvent;
    }

    public JHVRelatedEvents getEvent() {
        return event;
    }

    private static boolean containsPoint(Point p, int clickx, int clicky, int clickw, int clickh) {
        if (p != null) {
            return p.x >= clickx && p.x <= clickx + clickw && p.y >= clicky && p.y <= clicky + clickh;
        }
        return false;
    }

}
