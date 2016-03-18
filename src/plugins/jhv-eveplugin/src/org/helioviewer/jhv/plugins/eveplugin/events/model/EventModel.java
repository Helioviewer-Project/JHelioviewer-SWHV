package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
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
    private Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events;

    /** The event panel */
    private final EventPanel eventPanel;

    private final EventsSelectorElement eventSelectorElement;

    private boolean eventsActivated;

    private boolean prevNoPlotConfig;

    /**
     * Private default constructor.
     */
    private EventModel() {
        eventPlotConfiguration = new EventTypePlotConfiguration();
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        eventsVisible = false;
        eventPanel = new EventPanel();
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
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        if (!EVEState.getSingletonInstance().isMouseTimeIntervalDragging()) {
            createEventPlotConfiguration();
        }
    }

    @Override
    public void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events) {
        this.events = events;
        createEventPlotConfiguration();
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

    public JHVRelatedEvents getEventAtPosition(Point point) {
        if (eventPlotConfiguration != null) {
            return eventPlotConfiguration.getEventOnLocation(point);
        } else {
            return null;
        }
    }

    private ArrayList<Date> endDates = new ArrayList<Date>();
    private Date minimalEndDate = null;
    private Date maximumEndDate = null;
    private int minimalDateLine = 0;
    private int maximumDateLine = 0;
    private int nrLines = 0;
    private List<EventPlotConfiguration> plotConfig = new ArrayList<EventPlotConfiguration>();
    private int maxNrLines = 0;
    private Date tempLastDateWithData = null;
    private int maxEventLines = 0;

    public void backgroundWork() {

        final Map<JHVEventType, Integer> linesPerEventType = new HashMap<JHVEventType, Integer>();
        final Map<JHVEventType, List<EventPlotConfiguration>> eventPlotConfigPerEventType = new HashMap<JHVEventType, List<EventPlotConfiguration>>();

        if (events.size() > 0) {
            for (JHVEventType eventType : events.keySet()) {
                endDates = new ArrayList<Date>();
                plotConfig = new ArrayList<EventPlotConfiguration>();
                minimalEndDate = null;
                maximumEndDate = null;
                minimalDateLine = 0;
                maximumDateLine = 0;
                nrLines = 0;
                maxEventLines = 0;
                int relatedEventPosition = -1;
                SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap = events.get(eventType);
                for (Entry<SortedDateInterval, JHVRelatedEvents> evr : eventMap.entrySet()) {
                    handleEvent(evr.getValue(), relatedEventPosition, 0);
                }
                linesPerEventType.put(eventType, maxEventLines);
                maxNrLines += maxEventLines;
                eventPlotConfigPerEventType.put(eventType, plotConfig);
            }

            eventPlotConfiguration = new EventTypePlotConfiguration(events.size(), maxNrLines, linesPerEventType, eventPlotConfigPerEventType, tempLastDateWithData);
        } else {
            eventPlotConfiguration = new EventTypePlotConfiguration();
        }

        if (!eventPlotConfiguration.getEventPlotConfigurations().isEmpty() && prevNoPlotConfig) {
            prevNoPlotConfig = false;
        }
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
        } else {
            Log.debug("event plot configurations not visible");
        }
        endDates = new ArrayList<Date>();
        minimalEndDate = null;
        maximumEndDate = null;
        minimalDateLine = 0;
        maximumDateLine = 0;
        nrLines = 0;
        plotConfig = new ArrayList<EventPlotConfiguration>();
        maxNrLines = 0;
        tempLastDateWithData = null;
        maxEventLines = 0;
    }

    private boolean handleEvent(JHVRelatedEvents event, int relatedEventPosition, int relationNr) {
        EventPlotConfiguration epc = creatEventPlotConfiguration(event, relatedEventPosition, relationNr);
        plotConfig.add(epc);
        relatedEventPosition = epc.getEventPosition();
        return true;

    }

    private EventPlotConfiguration creatEventPlotConfiguration(JHVRelatedEvents event, int relatedEventPosition, int relationNr) {
        final Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();

        int eventPosition = 0;
        if (relatedEventPosition == -1 || (relatedEventPosition != -1 && relationNr > 0)) {
            if (minimalEndDate == null || minimalEndDate.getTime() >= event.getStart()) {
                minimalEndDate = new Date(event.getEnd());
                endDates.add(minimalEndDate);
                eventPosition = nrLines;
                nrLines++;
            } else {
                if (event.getStart() > maximumEndDate.getTime()) {
                    eventPosition = 0;
                    nrLines = 1;
                    endDates = new ArrayList<Date>();
                    endDates.add(new Date(event.getEnd()));
                } else {
                    eventPosition = minimalDateLine;
                    endDates.set(minimalDateLine, new Date(event.getEnd()));
                }
            }
        } else {
            endDates.set(relatedEventPosition, new Date(event.getEnd()));
        }

        minimalDateLine = defineMinimalDateLine(endDates);
        minimalEndDate = endDates.get(minimalDateLine);
        maximumDateLine = defineMaximumDateLine(endDates);
        maximumEndDate = endDates.get(maximumDateLine);
        double scaledX0 = defineScaledValue(event.getStart(), selectedInterval);
        double scaledX1 = defineScaledValue(event.getEnd(), selectedInterval);
        if (nrLines > maxEventLines) {
            maxEventLines = nrLines;
        }
        if (tempLastDateWithData == null || tempLastDateWithData.getTime() < (event.getEnd())) {
            tempLastDateWithData = new Date(event.getEnd());
        }
        return new EventPlotConfiguration(event, scaledX0, scaledX1, eventPosition);
    }

    private void createEventPlotConfiguration() {
        backgroundWork();
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

    private double defineScaledValue(long date, Interval<Date> selectedInterval) {
        double selectedDuration = 1.0 * (selectedInterval.getEnd().getTime() - selectedInterval.getStart().getTime());
        double position = 1.0 * (date - selectedInterval.getStart().getTime());
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
