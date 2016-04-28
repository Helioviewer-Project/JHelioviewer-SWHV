package org.helioviewer.jhv.plugins.eveplugin.events.model;

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

    private final int x0;
    private final int x1;

    private final int yPosition;

    public EventPlotConfiguration(JHVRelatedEvents event, int x0, int x1, int yPosition) {
        this.event = event;
        this.x0 = x0;
        this.x1 = x1;
        this.yPosition = yPosition;
    }

    public JHVRelatedEvents draw(Rectangle graphArea, Graphics2D g, int nrPreviousLines, Point mousePosition) {
        return draw(graphArea, event, x0, x1, yPosition, g, nrPreviousLines, mousePosition, true);
    }

    public static JHVRelatedEvents draw(Rectangle graphArea, JHVRelatedEvents event, int x0, int x1, int yPosition, Graphics2D g, int nrPreviousLines, Point mousePosition, boolean highlight) {
        JHVRelatedEvents highlightedEvent = null;
        int spacePerLine = 3;
        int startPosition = graphArea.y + spacePerLine * 2 * (nrPreviousLines + yPosition) + DrawConstants.EVENT_OFFSET;
        int y = startPosition;
        int w = Math.max(x1 - x0, 1);
        int h = spacePerLine;
        if (w < 5) {
            x0 = x0 - (5 / w);
            w = 5;
        }

        boolean containsMouse = containsPoint(mousePosition, x0 - 1, y - 1, w + 2, h + 2);
        boolean eventWasHightlighted = containsMouse || event.isHighlighted();
        if (mousePosition != null && containsMouse) {
            highlightedEvent = event;
        }

        if (eventWasHightlighted && highlight) {
            x0 = x0 - 10;
            y = y - 1;
            w = w + 20;
            h = h + 2;
            spacePerLine = h;
        }

        g.setColor(event.getColor());
        g.fillRect(x0, y, w, spacePerLine);

        return highlightedEvent;
    }

    private static boolean containsPoint(Point p, int clickx, int clicky, int clickw, int clickh) {
        if (p != null) {
            return p.x >= clickx && p.x <= clickx + clickw && p.y >= clicky && p.y <= clicky + clickh;
        }
        return false;
    }

}
