package org.helioviewer.plugins.eveplugin.events.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Default constructor.
     * 
     */
    public EventTypePlotConfiguration() {
        nrOfEventTypes = 0;
        totalNrLines = 0;
        maxLinesPerEventType = new HashMap<String, Integer>();
        eventPlotConfigurations = new HashMap<String, List<EventPlotConfiguration>>();
    }

    /**
     * Creates the event type plot configuration from the given number of
     * events, total number of lines, maximum lines per event type and the event
     * plot configurations.
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
            Map<String, List<EventPlotConfiguration>> eventPlotConfigurations) {
        this.nrOfEventTypes = nrOfEventTypes;
        this.totalNrLines = totalNrLines;
        this.maxLinesPerEventType = maxLinesPerEventType;
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

}
