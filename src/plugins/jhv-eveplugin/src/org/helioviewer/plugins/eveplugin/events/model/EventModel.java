package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.datatype.JHVEvent;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.events.data.EventRequesterListener;
import org.helioviewer.plugins.eveplugin.events.gui.EventPanel;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class EventModel implements ZoomControllerListener, EventRequesterListener {

    /** Singleton instance of the Event model */
    private static EventModel instance;

    /** the current selected interval */
    private Interval<Date> selectedInterval;

    /** the current available interval */
    private Interval<Date> availableInterval;

    /** event plot configurations */
    private EventTypePlotConfiguration eventPlotConfiguration;

    /** the interval lock */
    private final Object intervalLock;

    /** events visible */
    private boolean eventsVisible;

    /** current events */
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events;

    /** plotIdentifier */
    private String plot;

    /** The event panel */
    private final EventPanel eventPanel;

    /**
     * Private default constructor.
     */
    private EventModel() {
        intervalLock = new Object();
        eventPlotConfiguration = new EventTypePlotConfiguration();
        events = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
        eventsVisible = false;
        plot = PlotsContainerPanel.PLOT_IDENTIFIER_MASTER;
        eventPanel = new EventPanel();
    }

    /**
     * Gets the singleton instance of the EventModel.
     * 
     * @return the singleton instance of the event model
     */
    public static EventModel getSingletonInstance() {
        if (instance == null) {
            instance = new EventModel();
        }
        return instance;
    }

    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        synchronized (intervalLock) {
            availableInterval = newInterval;
        }
    }

    @Override
    public void selectedIntervalChanged(final Interval<Date> newInterval) {
        selectedInterval = newInterval;
        if (!EVEState.getSingletonInstance().isMouseTimeIntervalDragging()) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {

                    createEventPlotConfiguration();
                }
            });
        }
    }

    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
        synchronized (intervalLock) {

        }
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events) {
        synchronized (intervalLock) {
            eventPlotConfiguration = new EventTypePlotConfiguration();
            this.events = events;
            Log.info("New events received selected interval: " + selectedInterval + " availalble interval " + availableInterval);
            if (selectedInterval != null && availableInterval != null) {
                createEventPlotConfiguration();
            }
        }
        if (selectedInterval != null && availableInterval != null) {
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel, plot);
        }
    }

    public EventTypePlotConfiguration getEventTypePlotConfiguration() {
        synchronized (intervalLock) {
            return eventPlotConfiguration;
        }
    }

    public boolean isEventsVisible() {
        return eventsVisible;
    }

    public void setEventsVisible(boolean visible) {
        eventsVisible = visible;
    }

    public void setPlotIdentifier(String plotIdentifier) {
        if (!plot.equals(plotIdentifier)) {
            DrawController.getSingletonInstance().removeDrawableElement(eventPanel, plot);
            DrawController.getSingletonInstance().addDrawableElement(eventPanel, plotIdentifier);
            plot = plotIdentifier;
        }
    }

    private void createEventPlotConfiguration() {
        Log.info("Start printing events");
        int maxNrLines = 0;
        Map<String, Integer> linesPerEventType = new HashMap<String, Integer>();
        Map<String, List<EventPlotConfiguration>> eventPlotConfigPerEventType = new HashMap<String, List<EventPlotConfiguration>>();
        for (String eventType : events.keySet()) {
            ArrayList<Date> endDates = new ArrayList<Date>();
            List<EventPlotConfiguration> plotConfig = new ArrayList<EventPlotConfiguration>();
            Date minimalEndDate = null;
            Date maximumEndDate = null;
            int minimalDateLine = 0;
            int maximumDateLine = 0;
            int nrLines = 0;
            int maxEventLines = 0;
            Log.info("eventType :" + eventType);
            for (Date sDate : events.get(eventType).keySet()) {
                Log.info("Start date :" + sDate);
                for (Date eDate : events.get(eventType).get(sDate).keySet()) {
                    Log.info("End date " + eDate);
                    for (JHVEvent event : events.get(eventType).get(sDate).get(eDate)) {
                        int eventPosition = 0;
                        if (minimalEndDate == null || minimalEndDate.compareTo(event.getStartDate()) >= 0) {
                            // first event or event start before minimal end
                            // date so next line
                            minimalEndDate = event.getEndDate();
                            endDates.add(event.getEndDate());
                            eventPosition = nrLines;
                            nrLines++;
                        } else {
                            if (event.getStartDate().after(maximumEndDate)) {
                                // After all other events so start new line and
                                // reset everything
                                eventPosition = 0;
                                nrLines = 1;
                                endDates = new ArrayList<Date>();
                                endDates.add(event.getEndDate());
                            } else {
                                // After minimal date so after minimal end date
                                eventPosition = minimalDateLine;
                                endDates.set(minimalDateLine, event.getEndDate());
                            }
                        }
                        minimalDateLine = defineMinimalDateLine(endDates);
                        minimalEndDate = endDates.get(minimalDateLine);
                        maximumDateLine = defineMaximumDateLine(endDates);
                        maximumEndDate = endDates.get(maximumDateLine);
                        double scaledX0 = defineScaledValue(event.getStartDate());
                        double scaledX1 = defineScaledValue(event.getEndDate());
                        if (nrLines > maxEventLines) {
                            maxEventLines = nrLines;
                        }
                        plotConfig.add(new EventPlotConfiguration(event, scaledX0, scaledX1, eventPosition));
                    }
                }
            }
            linesPerEventType.put(eventType, maxEventLines);
            maxNrLines += maxEventLines;
            eventPlotConfigPerEventType.put(eventType, plotConfig);
        }
        eventPlotConfiguration = new EventTypePlotConfiguration(events.size(), maxNrLines, linesPerEventType, eventPlotConfigPerEventType);
    }

    private int defineMaximumDateLine(ArrayList<Date> endDates) {
        Date maxDate = null;
        int maxLine = 0;
        for (Date d : endDates) {
            if (maxDate == null) {
                // first case
                maxDate = d;
                maxLine = 0;
            } else {
                // the rest
                if (d.after(maxDate)) {
                    maxDate = d;
                    maxLine = endDates.indexOf(d);
                }
            }
        }
        return maxLine;
    }

    private int defineMinimalDateLine(ArrayList<Date> endDates) {
        Date minDate = null;
        int minLine = 0;
        for (Date d : endDates) {
            if (minDate == null) {
                // first case
                minDate = d;
                minLine = 0;
            } else {
                // the rest
                if (d.before(minDate)) {
                    minDate = d;
                    minLine = endDates.indexOf(d);
                }
            }
        }
        return minLine;
    }

    private double defineScaledValue(Date date) {
        double selectedDuration = 1.0 * (selectedInterval.getEnd().getTime() - selectedInterval.getStart().getTime());
        double position = 1.0 * (date.getTime() - selectedInterval.getStart().getTime());
        return position / selectedDuration;
    }

}
