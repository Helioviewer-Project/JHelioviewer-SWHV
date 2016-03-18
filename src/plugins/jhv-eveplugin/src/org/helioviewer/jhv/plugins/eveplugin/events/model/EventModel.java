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
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.plugins.eveplugin.EVEState;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventPanel;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventsSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

/**
 *
 * <pre>
 * "                              _,,aaaaa,,_                           "
 * "                           _,dP"''    `""""Ya,_                     "
 * "                        ,aP"'                `"Yb,_                 "
 * "                      ,8"'                       `"8a,              "
 * "                    ,8"                             `"8,_           "
 * "                  ,8"                                  "Yb,         "
 * "                ,8"                                      `8,        "
 * "               dP'                                        8I        "
 * "             ,8"                           bg,_          ,P'        "
 * "            ,8'                              "Y8"Ya,,,,ad"          "
 * "           ,d"                            a,_ I8   `"""'            "
 * "          ,8'                              ""888                    "
 * "          dP     __                           `Yb,                  "
 * "         dP'  _,d8P::::Y8b,                     `Ya                 "
 * "    ,adba8',d88P::;;::;;;:"b:::Ya,_               Ya                "
 * "   dP":::"Y88P:;P"""YP"""Yb;::::::"Ya,             "Y,              "
 * "   8:::::::Yb;d" _  "_    dI:::::::::"Yb,__,,gd88ba,db              "
 * "   Yb:::::::"8(,8P _d8   d8:::::::::::::Y88P"::::::Y8I              "
 * "   `Yb;:::::::""::"":b,,dP::::::::::::::::::;aaa;:::8(              "
 * "     `Y8a;:::::::::::::::::::::;;::::::::::8P""Y8)::8I              "
 * "       8b"ba::::::::::::::::;adP:::::::::::":::dP::;8'              "
 * "       `8b;::::::::::::;aad888P::::::::::::::;dP::;8'               "
 * "        `8b;::::::::""""88"  d::::::::::b;:::::;;dP'                "
 * "          "Yb;::::::::::Y8bad::::::::::;"8Paaa""'                   "
 * "            `"Y8a;;;:::::::::::::;;aadP""                           "
 * "                ``""Y88bbbdddd88P""8b,                              "
 * "                         _,d8"::::::"8b,                            "
 * "                       ,dP8"::::::;;:::"b,                          "
 * "                     ,dP"8:::::::Yb;::::"b,                         "
 * "                   ,8P:dP:::::::::Yb;::::"b,                        "
 * "                _,dP:;8":::::::::::Yb;::::"b                        "
 * "      ,aaaaaa,,d8P:::8":::::::::::;dP:::::;8                        "
 * "   ,ad":;;:::::"::::8"::::::::::;dP::::::;dI                        "
 * "  dP";adP":::::;:;dP;::::aaaad88"::::::adP:8b,___                   "
 * " d8:::8;;;aadP"::8'Y8:d8P"::::::::::;dP";d"'`Yb:"b                  "
 * " 8I:::;""":::::;dP I8P"::::::::::;a8"a8P"     "b:P                  "
 * " Yb::::"8baa8"""'  8;:;d"::::::::d8P"'         8"                   "
 * "  "YbaaP::8;P      `8;d::;a::;;;;dP           ,8                    "
 * "     `"Y8P"'         Yb;;d::;aadP"           ,d'                    "
 * "                      "YP:::"P'             ,d'                     "
 * "                        "8bdP'    _        ,8'                      "
 * "       Normand         ,8"`""Yba,d"      ,d"                        "
 * "       Veilleux       ,P'     d"8'     ,d"                          "
 * "                     ,8'     d'8'     ,P'                           "
 * "                     (b      8 I      8,                            "
 * "                      Y,     Y,Y,     `b,                           "
 * "                ____   "8,__ `Y,Y,     `Y""b,                       "
 * "            ,adP""""b8P""""""""Ybdb,        Y,                      "
 * "          ,dP"    ,dP'            `""       `8                      "
 * "         ,8"     ,P'                        ,P                      "
 * "         8'      8)                        ,8'                      "
 * "         8,      Yb                      ,aP'                       "
 * "         `Ya      Yb                  ,ad"'                         "
 * "           "Ya,___ "Ya             ,ad"'                            "
 * "             ``""""""`Yba,,,,,,,adP"'                               "
 * "                        `"""""""'                                   "
 * </pre>
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventModel implements TimingListener, JHVEventHandler {

    /** Singleton instance of the Event model */
    private static EventModel instance;

    /** event plot configurations */
    private EventTypePlotConfiguration eventPlotConfiguration;

    /** Instance of the event container */
    private final JHVEventContainer eventContainer;

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
        eventContainer = JHVEventContainer.getSingletonInstance();
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
        Interval<Date> availableInterval = DrawController.getSingletonInstance().getAvailableInterval();
        eventContainer.requestForInterval(availableInterval.getStart(), availableInterval.getEnd(), EventModel.this);
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
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            DrawController.getSingletonInstance().updateDrawableElement(eventPanel);
        } else {
            Log.debug("event plot configurations not visible");
        }
    }

    public EventTypePlotConfiguration getEventTypePlotConfiguration() {
        createEventPlotConfiguration();
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

    @Override
    public void cacheUpdated() {
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        eventContainer.requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), this);
    }

}
