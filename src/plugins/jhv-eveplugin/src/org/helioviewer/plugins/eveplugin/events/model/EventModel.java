package org.helioviewer.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.draw.DrawController;
import org.helioviewer.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.plugins.eveplugin.events.data.EventRequesterListener;
import org.helioviewer.plugins.eveplugin.events.gui.EventPanel;
import org.helioviewer.plugins.eveplugin.events.gui.EventsSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

/**
 *
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventModel implements TimingListener, EventRequesterListener {

    /** Singleton instance of the Event model */
    private static EventModel instance;

    /** event plot configurations */
    private EventTypePlotConfiguration eventPlotConfiguration;

    /** events visible */
    private boolean eventsVisible;

    /** current events */
    private Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events;

    /** The event panel */
    private final EventPanel eventPanel;

    /** The swing worker creating the event type plot configurations */
    private SwingWorker<EventTypePlotConfiguration, Void> currentSwingWorker;

    private final EventsSelectorElement eventSelectorElement;

    private final List<EventModelListener> listeners;

    private boolean eventsActivated;

    private boolean prevNoPlotConfig;

    /**
     * Private default constructor.
     */
    private EventModel() {
        eventPlotConfiguration = new EventTypePlotConfiguration();
        events = new HashMap<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>>();
        eventsVisible = false;
        eventPanel = new EventPanel();
        currentSwingWorker = null;
        eventSelectorElement = new EventsSelectorElement(this);
        listeners = new ArrayList<EventModelListener>();
        eventsActivated = false;
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

    /**
     * Adds an event model listener to the event model.
     *
     * @param listener
     *            the listener to add
     */
    public void addEventModelListener(EventModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        if (!EVEState.getSingletonInstance().isMouseTimeIntervalDragging()) {
            createEventPlotConfiguration();
        }
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events) {
        this.events = events;
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        Interval<Date> availableInterval = DrawController.getSingletonInstance().getAvailableInterval();
        if (selectedInterval != null && availableInterval != null) {
            createEventPlotConfiguration();
        }
    }

    public EventTypePlotConfiguration getEventTypePlotConfiguration() {

        if (eventPlotConfiguration != null) {
            return eventPlotConfiguration;
        } else {
            return new EventTypePlotConfiguration();
        }

    }

    public boolean isEventsVisible() {
        return eventsVisible;
    }

    public void setEventsVisible(boolean visible) {
        if (eventsVisible != visible) {
            eventsVisible = visible;
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
            LineDataSelectorModel.getSingletonInstance().lineDataElementUpdated(eventSelectorElement);
        }

    }

    public void deactivateEvents() {
        if (eventsActivated) {
            eventsVisible = false;
            eventsActivated = false;
            DrawController.getSingletonInstance().removeDrawableElement(eventPanel);
            // LineDataSelectorModel.getSingletonInstance().removeLineData(eventSelectorElement);
            fireEventsDeactivated();
        }
    }

    public void activateEvents() {
        if (!eventsActivated) {
            eventsVisible = true;
            eventsActivated = true;
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
            // LineDataSelectorModel.getSingletonInstance().addLineData(eventSelectorElement);
        }
    }

    public void setPlotIdentifier(String plotIdentifier) {
    }

    public JHVEvent getEventAtPosition(Point point) {
        if (eventPlotConfiguration != null) {
            return eventPlotConfiguration.getEventOnLocation(point);
        } else {
            return null;
        }
    }

    private void createEventPlotConfiguration() {

        if (currentSwingWorker != null) {
            currentSwingWorker.cancel(true);
        }
        final Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();

        currentSwingWorker = new SwingWorker<EventTypePlotConfiguration, Void>() {

            @Override
            public EventTypePlotConfiguration doInBackground() {
                Thread.currentThread().setName("EventModel--EVE");
                Set<String> uniqueIDs = new HashSet<String>();
                int maxNrLines = 0;
                Map<String, Integer> linesPerEventType = new HashMap<String, Integer>();
                Map<String, List<EventPlotConfiguration>> eventPlotConfigPerEventType = new HashMap<String, List<EventPlotConfiguration>>();
                Date tempLastDateWithData = null;
                if (events.size() > 0) {
                    for (String eventType : events.keySet()) {
                        ArrayList<Date> endDates = new ArrayList<Date>();
                        List<EventPlotConfiguration> plotConfig = new ArrayList<EventPlotConfiguration>();
                        Date minimalEndDate = null;
                        Date maximumEndDate = null;
                        int minimalDateLine = 0;
                        int maximumDateLine = 0;
                        int nrLines = 0;
                        int maxEventLines = 0;
                        for (Date sDate : events.get(eventType).keySet()) {
                            for (Date eDate : events.get(eventType).get(sDate).keySet()) {
                                for (JHVEvent event : events.get(eventType).get(sDate).get(eDate)) {
                                    if (!uniqueIDs.contains(event.getUniqueID())) {
                                        uniqueIDs.add(event.getUniqueID());
                                        int eventPosition = 0;
                                        if (minimalEndDate == null || minimalEndDate.compareTo(event.getStartDate()) >= 0) {
                                            // first event or event start before
                                            // minimal end
                                            // date so next line
                                            minimalEndDate = event.getEndDate();
                                            endDates.add(event.getEndDate());
                                            eventPosition = nrLines;
                                            nrLines++;
                                        } else {
                                            if (event.getStartDate().after(maximumEndDate)) {
                                                // After all other events so
                                                // start
                                                // new line
                                                // and
                                                // reset everything
                                                eventPosition = 0;
                                                nrLines = 1;
                                                endDates = new ArrayList<Date>();
                                                endDates.add(event.getEndDate());
                                            } else {
                                                // After minimal date so after
                                                // minimal end
                                                // date
                                                eventPosition = minimalDateLine;
                                                endDates.set(minimalDateLine, event.getEndDate());
                                            }
                                        }

                                        minimalDateLine = defineMinimalDateLine(endDates);
                                        minimalEndDate = endDates.get(minimalDateLine);
                                        maximumDateLine = defineMaximumDateLine(endDates);
                                        maximumEndDate = endDates.get(maximumDateLine);
                                        double scaledX0 = defineScaledValue(event.getStartDate(), selectedInterval);
                                        double scaledX1 = defineScaledValue(event.getEndDate(), selectedInterval);
                                        if (nrLines > maxEventLines) {
                                            maxEventLines = nrLines;
                                        }
                                        if (tempLastDateWithData == null || tempLastDateWithData.before(event.getEndDate())) {
                                            tempLastDateWithData = event.getEndDate();
                                        }
                                        event.addHighlightListener(DrawController.getSingletonInstance());
                                        plotConfig.add(new EventPlotConfiguration(event, scaledX0, scaledX1, eventPosition));
                                    } else {
                                        // Log.debug("Event with unique ID : " +
                                        // event.getUniqueID() + "not drawn");
                                    }
                                }
                            }
                        }
                        linesPerEventType.put(eventType, maxEventLines);
                        maxNrLines += maxEventLines;
                        eventPlotConfigPerEventType.put(eventType, plotConfig);
                    }

                    return new EventTypePlotConfiguration(events.size(), maxNrLines, linesPerEventType, eventPlotConfigPerEventType, tempLastDateWithData);
                } else {
                    return new EventTypePlotConfiguration();
                }
            }

            @Override
            public void done() {
                try {
                    if (!isCancelled()) {
                        eventPlotConfiguration = get();
                        if (eventPlotConfiguration.getEventPlotConfigurations().size() != 0 && prevNoPlotConfig) {
                            prevNoPlotConfig = false;
                        }
                        if (eventPlotConfiguration != null && EventModel.getSingletonInstance().isEventsVisible()) {
                            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
                        } else {

                            if (eventPlotConfiguration == null) {
                                Log.debug("event plot configurations null");
                            } else {
                                Log.debug("event plot configurations not visible");
                            }
                        }
                        // }
                    } else {
                        // Log.info("Worker was cancelled");
                    }
                } catch (InterruptedException e) {
                    Log.error("Could not create the event type plot configurations" + e.getMessage());
                    Log.error("The error" + e.getMessage());
                } catch (ExecutionException e) {
                    Log.error("Could not create the event type plot configurations" + e.getMessage());
                    Log.error("The error" + e.getMessage());
                }
            }
        };
        currentSwingWorker.execute();
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

    private double defineScaledValue(Date date, Interval<Date> selectedInterval) {
        double selectedDuration = 1.0 * (selectedInterval.getEnd().getTime() - selectedInterval.getStart().getTime());
        double position = 1.0 * (date.getTime() - selectedInterval.getStart().getTime());
        return position / selectedDuration;
    }

    private void fireEventsDeactivated() {
        for (EventModelListener l : listeners) {
            l.eventsDeactivated();
        }
    }

    public boolean hasElementsToDraw() {
        boolean tempPrevZero = prevNoPlotConfig;
        if (eventPlotConfiguration.getEventPlotConfigurations().isEmpty()) {
            prevNoPlotConfig = true;
        }
        return !tempPrevZero || !eventPlotConfiguration.getEventPlotConfigurations().isEmpty();
    }
}
