package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.plugins.eveplugin.EVEState;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.events.data.EventRequesterListener;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventPanel;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventsSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

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
        eventsActivated = false;
        LineDataSelectorModel.getSingletonInstance().addLineData(eventSelectorElement);
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
        }
    }

    public void activateEvents() {
        if (!eventsActivated) {
            eventsVisible = true;
            eventsActivated = true;
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
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

            private final Set<String> uniqueIDs = new HashSet<String>();
            private final Map<String, Integer> eventLocations = new HashMap<String, Integer>();
            private int maxNrLines = 0;
            private final Map<String, Integer> linesPerEventType = new HashMap<String, Integer>();
            private final Map<String, List<EventPlotConfiguration>> eventPlotConfigPerEventType = new HashMap<String, List<EventPlotConfiguration>>();
            private Date tempLastDateWithData = null;

            private ArrayList<Date> endDates = new ArrayList<Date>();
            private List<EventPlotConfiguration> plotConfig = new ArrayList<EventPlotConfiguration>();
            private Date minimalEndDate = null;
            private Date maximumEndDate = null;
            private int minimalDateLine = 0;
            private int maximumDateLine = 0;
            private int nrLines = 0;
            private int maxEventLines = 0;

            @Override
            public EventTypePlotConfiguration doInBackground() {
                Thread.currentThread().setName("EventModel--EVE");
                if (events.size() > 0) {
                    for (String eventType : events.keySet()) {
                        endDates = new ArrayList<Date>();
                        plotConfig = new ArrayList<EventPlotConfiguration>();
                        minimalEndDate = null;
                        maximumEndDate = null;
                        minimalDateLine = 0;
                        maximumDateLine = 0;
                        nrLines = 0;
                        maxEventLines = 0;
                        int relatedEventPosition = -1;
                        for (Date sDate : events.get(eventType).keySet()) {
                            for (Date eDate : events.get(eventType).get(sDate).keySet()) {
                                for (JHVEvent event : events.get(eventType).get(sDate).get(eDate)) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        return new EventTypePlotConfiguration();
                                    } else {
                                        handleEvent(event, relatedEventPosition, 0);
                                    }
                                }
                            }
                            linesPerEventType.put(eventType, maxEventLines);
                            maxNrLines += maxEventLines;
                            eventPlotConfigPerEventType.put(eventType, plotConfig);
                        }
                    }

                    return new EventTypePlotConfiguration(events.size(), maxNrLines, linesPerEventType, eventPlotConfigPerEventType, tempLastDateWithData);
                } else {
                    return new EventTypePlotConfiguration();
                }
            }

            private boolean handleEvent(JHVEvent event, int relatedEventPosition, int relationNr) {
                if (!uniqueIDs.contains(event.getUniqueID())) {
                    EventPlotConfiguration epc = creatEventPlotConfiguration(event, relatedEventPosition, relationNr);
                    plotConfig.add(epc);
                    relatedEventPosition = epc.getEventPosition();
                    int localRelationNr = 0;
                    for (JHVEventRelation jer : event.getEventRelationShip().getNextEvents().values()) {
                        if (jer.getTheEvent() != null) {
                            if (handleEvent(jer.getTheEvent(), relatedEventPosition, localRelationNr)) {
                                localRelationNr++;
                            }
                        }
                    }
                    for (JHVEventRelation jer : event.getEventRelationShip().getPrecedingEvents().values()) {
                        if (jer.getTheEvent() != null) {
                            if (handleEvent(jer.getTheEvent(), relatedEventPosition, localRelationNr)) {
                                localRelationNr++;
                            }
                        }
                    }
                    TreeMap<Date, JHVEvent> sortedRelatedEvents = new TreeMap<Date, JHVEvent>();
                    for (JHVEventRelation jer : event.getEventRelationShip().getRelatedEventsByRule().values()) {
                        if (jer.getTheEvent() != null) {
                            sortedRelatedEvents.put(jer.getTheEvent().getStartDate(), jer.getTheEvent());
                        }
                    }
                    for (JHVEvent relEvent : sortedRelatedEvents.values()) {
                        if (relEvent.getJHVEventType().equals(event.getJHVEventType())) {
                            if (!uniqueIDs.contains(relEvent.getUniqueID())) {
                                plotConfig.add(creatEventPlotConfiguration(relEvent, relatedEventPosition, localRelationNr));
                            }
                        }

                    }
                    return true;
                } else {
                    return false;
                }

            }

            private EventPlotConfiguration creatEventPlotConfiguration(JHVEvent event, int relatedEventPosition, int relationNr) {
                uniqueIDs.add(event.getUniqueID());
                int eventPosition = 0;
                if (relatedEventPosition == -1 || (relatedEventPosition != -1 && relationNr > 0)) {
                    if (minimalEndDate == null || minimalEndDate.compareTo(event.getStartDate()) >= 0) {
                        // first event or event start
                        // before
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
                            // After minimal date so
                            // after
                            // minimal end
                            // date
                            eventPosition = minimalDateLine;
                            endDates.set(minimalDateLine, event.getEndDate());
                        }
                    }
                } else {
                    eventPosition = relatedEventPosition;
                    endDates.set(relatedEventPosition, event.getEndDate());
                }

                eventLocations.put(event.getUniqueID(), eventPosition);
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
                return new EventPlotConfiguration(event, scaledX0, scaledX1, eventPosition);
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

                    }
                } catch (InterruptedException e) {
                    Log.error("Could not create the event type plot configurations" + e.getMessage());
                    Log.error("The error" + e.getMessage());
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    Log.error("Could not create the event type plot configurations" + e.getMessage());
                    Log.error("The error" + e.getMessage());
                    e.printStackTrace();
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

    public boolean hasElementsToDraw() {
        boolean tempPrevZero = prevNoPlotConfig;
        if (eventPlotConfiguration.getEventPlotConfigurations().isEmpty()) {
            prevNoPlotConfig = true;
        }
        return !tempPrevZero || !eventPlotConfiguration.getEventPlotConfigurations().isEmpty();
    }

}
