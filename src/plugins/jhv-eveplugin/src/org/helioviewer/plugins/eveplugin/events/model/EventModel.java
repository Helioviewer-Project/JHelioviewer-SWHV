package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private List<EventPlotConfiguration> eventPlotConfiguration;

    /** the interval lock */
    private final Object intervalLock;

    /** events visible */
    private boolean eventsVisible;

    /** current events */
    private List<JHVEvent> events;

    /** plotIdentifier */
    private String plot;

    /** The event panel */
    private final EventPanel eventPanel;

    /**
     * Private default constructor.
     */
    private EventModel() {
        intervalLock = new Object();
        eventPlotConfiguration = new ArrayList<EventPlotConfiguration>();
        events = new ArrayList<JHVEvent>();
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
    public void newEventsReceived(List<JHVEvent> events) {
        synchronized (intervalLock) {
            eventPlotConfiguration = new ArrayList<EventPlotConfiguration>();
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

    public List<EventPlotConfiguration> getEventPlotConfiguration() {
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
        for (JHVEvent event : events) {
            double scaledX0 = defineScaledValue(event.getStartDate());
            double scaledX1 = defineScaledValue(event.getEndDate());
            eventPlotConfiguration.add(new EventPlotConfiguration(event, scaledX0, scaledX1));
        }
    }

    private double defineScaledValue(Date date) {
        double selectedDuration = 1.0 * (selectedInterval.getEnd().getTime() - selectedInterval.getStart().getTime());
        double position = 1.0 * (date.getTime() - selectedInterval.getStart().getTime());
        return position / selectedDuration;
    }

}
