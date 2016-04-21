package org.helioviewer.jhv.plugins.eveplugin.events.model;

import java.awt.Point;
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
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventPanel;
import org.helioviewer.jhv.plugins.eveplugin.events.gui.EventsSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

/*
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class EventModel implements TimingListener, JHVEventHandler {

    private static EventModel instance;
    private final JHVEventContainer eventContainer;
    private boolean eventsVisible;
    private Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events;
    private final EventPanel eventPanel;

    private final EventsSelectorElement eventSelectorElement;

    private boolean eventsActivated;

    private JHVRelatedEvents eventUnderMouse;

    private EventModel() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        eventsVisible = false;
        eventPanel = new EventPanel();
        eventSelectorElement = new EventsSelectorElement(this);
        eventsActivated = false;
        LineDataSelectorModel.getSingletonInstance().addLineData(eventSelectorElement);
    }

    public static EventModel getSingletonInstance() {
        if (instance == null) {
            instance = new EventModel();
        }
        return instance;
    }

    @Override
    public void availableIntervalChanged() {
        Interval availableInterval = EVEPlugin.dc.getAvailableInterval();
        eventContainer.requestForInterval(availableInterval.start, availableInterval.end, EventModel.this);
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
    }

    @Override
    public void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events) {
        this.events = events;
        if (EventModel.getSingletonInstance().isEventsVisible()) {
            EVEPlugin.dc.updateDrawableElement(eventPanel, true);
        } else {
            Log.debug("event plot configurations not visible");
        }
    }

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> getEvents() {
        return events;
    }

    public boolean isEventsVisible() {
        return eventsVisible;
    }

    public void setEventsVisible(boolean visible) {
        if (eventsVisible != visible) {
            eventsVisible = visible;
            EVEPlugin.dc.updateDrawableElement(eventPanel, true);
            LineDataSelectorModel.getSingletonInstance().lineDataElementUpdated(eventSelectorElement);
        }
    }

    public void deactivateEvents() {
        if (eventsActivated) {
            eventsVisible = false;
            eventsActivated = false;
            EVEPlugin.dc.removeDrawableElement(eventPanel);
        }
    }

    public void activateEvents() {
        if (!eventsActivated) {
            eventsVisible = true;
            eventsActivated = true;
            EVEPlugin.dc.updateDrawableElement(eventPanel, true);
        }
    }

    public JHVRelatedEvents getEventAtPosition(Point point) {
        return null;
    }

    public long getLastDateWithData() {
        return -1;
    }

    public boolean hasElementsToDraw() {
        return true;
    }

    @Override
    public void cacheUpdated() {
        Interval selectedInterval = EVEPlugin.dc.getSelectedInterval();
        eventContainer.requestForInterval(selectedInterval.start, selectedInterval.end, this);
        EVEPlugin.dc.updateDrawableElement(eventPanel, true);
    }

    public JHVRelatedEvents getEventUnderMouse() {
        return eventUnderMouse;
    }

    public void setEventUnderMouse(JHVRelatedEvents event) {
        eventUnderMouse = event;
    }

}
