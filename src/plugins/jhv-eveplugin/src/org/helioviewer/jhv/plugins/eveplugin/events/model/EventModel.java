package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Point;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    // private final EventTypePlotConfiguration eventPlotConfiguration;

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

    // private boolean prevNoPlotConfig;

    // private Date lastDateWithData;

    private JHVRelatedEvents eventUnderMouse;

    /**
     * Private default constructor.
     */
    private EventModel() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        // eventPlotConfiguration = new EventTypePlotConfiguration();
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
        /*
        if (!EVEState.getSingletonInstance().isMouseTimeIntervalDragging()) {
            // createEventPlotConfiguration();
        }
        */
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

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> getEvents() {
        return events;
    }

    /*
     * public EventTypePlotConfiguration getEventTypePlotConfiguration() {
     * createEventPlotConfiguration(); if (eventPlotConfiguration != null) {
     * return eventPlotConfiguration; } else { return new
     * EventTypePlotConfiguration(); } }
     */

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
        // TODO find solution for this problem...
        /*
         * if (eventPlotConfiguration != null) { return
         * eventPlotConfiguration.getEventOnLocation(point); } else { return
         * null; }
         */
        return null;
    }

    public Date getLastDateWithData() {
        return null; // lastDateWithData;
    }

    public boolean hasElementsToDraw() {
        /*
         * boolean tempPrevZero = prevNoPlotConfig; if
         * (eventPlotConfiguration.getEventPlotConfigurations().isEmpty()) {
         * prevNoPlotConfig = true; } return !tempPrevZero ||
         * !eventPlotConfiguration.getEventPlotConfigurations().isEmpty();
         */
        // TODO find better solution for this
        return true;
    }

    @Override
    public void cacheUpdated() {
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        eventContainer.requestForInterval(selectedInterval.getStart(), selectedInterval.getEnd(), this);
        DrawController.getSingletonInstance().fireRedrawRequest();
    }

    public JHVRelatedEvents getEventUnderMouse() {
        return eventUnderMouse;
    }

    public void setEventUnderMouse(JHVRelatedEvents event) {
        eventUnderMouse = event;
    }

}
