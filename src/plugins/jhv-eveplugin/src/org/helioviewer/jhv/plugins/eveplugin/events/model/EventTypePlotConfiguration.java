package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

/**
 * Combines everything needed to draw the events on the eve plugin.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventTypePlotConfiguration {
    /** Number of events in this event type plot configuration */
    private final int nrOfEventTypes;

    /** The plot configurations for all events ordered per event type */
    private final Map<JHVEventType, List<EventPlotConfiguration>> eventPlotConfigurations;

    /**
     * Default constructor.
     *
     */
    public EventTypePlotConfiguration() {
        nrOfEventTypes = 0;
        eventPlotConfigurations = new HashMap<JHVEventType, List<EventPlotConfiguration>>();
    }

    /**
     * Creates the event type plot configuration from the given number of
     * events, total number of lines, maximum lines per event type, the event
     * plot configurations and the color of the event type.
     *
     * @param nrOfEventType
     *            the total number of event types to be drawn
     * @param totalNrOfLines
     *            the total number of lines needed by all event types
     * @param maxLinesPerEventType
     *            overview of the total number of lines per event type
     * @param eventPlotConfigurations
     *            the event plot configurations
     */
    public EventTypePlotConfiguration(int nrOfEventTypes, Map<JHVEventType, List<EventPlotConfiguration>> eventPlotConfigurations) {
        this.nrOfEventTypes = nrOfEventTypes;
        this.eventPlotConfigurations = eventPlotConfigurations;
    }

    /**
     * Gets the number of event types in this configuration
     *
     * @return the number of Event types
     */
    public int getNrOfEventTypes() {
        return nrOfEventTypes;
    }

    /**
     * Gets the event plot configurations.
     *
     * @return the event plot configurations
     */
    public Map<JHVEventType, List<EventPlotConfiguration>> getEventPlotConfigurations() {
        return eventPlotConfigurations;
    }

    /**
     * Gets the event at the given location.
     *
     * @param p
     *            the location to check.
     * @return null if no event was found, or the event if found
     */
    public JHVRelatedEvents getEventOnLocation(Point p) {
        for (List<EventPlotConfiguration> value : eventPlotConfigurations.values()) {
            for (EventPlotConfiguration epc : value) {
                JHVRelatedEvents event = epc.getEventAtPoint(p);
                if (event != null) {
                    return event;
                }
            }
        }
        return null;
    }

}
