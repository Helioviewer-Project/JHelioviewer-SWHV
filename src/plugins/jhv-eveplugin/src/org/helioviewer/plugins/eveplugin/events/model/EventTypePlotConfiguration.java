package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

/**
 * Combines everything needed to draw the events on the eve plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventTypePlotConfiguration {
    /** Number of events in this event type plot configuration */
    private final int nrOfEventTypes;

    /** Maximum number of lines needed for all events */
    private final int totalNrLines;

    /** Maximum of lines for every event type */
    private final Map<String, Integer> maxLinesPerEventType;

    /** The plot configurations for all events ordered per event type */
    private final Map<String, List<EventPlotConfiguration>> eventPlotConfigurations;

    private final Date lastDateWithData;

    /**
     * Default constructor.
     * 
     */
    public EventTypePlotConfiguration() {
        nrOfEventTypes = 0;
        totalNrLines = 0;
        maxLinesPerEventType = new HashMap<String, Integer>();
        eventPlotConfigurations = new HashMap<String, List<EventPlotConfiguration>>();
        lastDateWithData = null;
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
    public EventTypePlotConfiguration(int nrOfEventTypes, int totalNrLines, Map<String, Integer> maxLinesPerEventType,
            Map<String, List<EventPlotConfiguration>> eventPlotConfigurations, Date lastDateWithData) {
        this.nrOfEventTypes = nrOfEventTypes;
        this.totalNrLines = totalNrLines;
        this.maxLinesPerEventType = maxLinesPerEventType;
        this.eventPlotConfigurations = eventPlotConfigurations;
        this.lastDateWithData = lastDateWithData;
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
     * Gets the total number of lines.
     * 
     * @return the total number lines
     */
    public int getTotalNrLines() {
        return totalNrLines;
    }

    /**
     * Gets the maximum lines per event type.
     * 
     * @return the max lines per event type
     */
    public Map<String, Integer> getMaxLinesPerEventType() {
        return maxLinesPerEventType;
    }

    /**
     * Gets the event plot configurations.
     * 
     * @return the event plot configurations
     */
    public Map<String, List<EventPlotConfiguration>> getEventPlotConfigurations() {
        return eventPlotConfigurations;
    }

    /**
     * Gets the event at the given location.
     * 
     * @param p
     *            the location to check.
     * @return null if no event was found, or the event if found
     */
    public JHVEvent getEventOnLocation(Point p) {
        for (String s : eventPlotConfigurations.keySet()) {
            for (EventPlotConfiguration epc : eventPlotConfigurations.get(s)) {
                JHVEvent event = epc.getEventAtPoint(p);
                if (event != null) {
                    return event;
                }
            }
        }
        return null;
    }

    public Date getLastDateWithData() {
        return lastDateWithData;
    }
}
