package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.container.cache.JHVCacheColors;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;

public class JHVRelatedEvents {

    private static final HashSet<JHVEventHighlightListener> listeners = new HashSet<JHVEventHighlightListener>();

    private final ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
    private final SortedDateInterval interval = new SortedDateInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    private final ArrayList<JHVAssociation> associations = new ArrayList<JHVAssociation>();

    private final JHVEventType eventType;
    private final Color color;
    private boolean highlighted;

    public JHVRelatedEvents(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        eventType = event.getJHVEventType();
        color = JHVCacheColors.getNextColor();
        highlighted = false;

        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        interval.start = event.start;
        interval.end = event.end;
        events.add(event);
        eventsMap.get(eventType).put(interval, this);
    }

    public ArrayList<JHVEvent> getEvents() {
        return events;
    }

    public long getEnd() {
        return interval.end;
    }

    public long getStart() {
        return interval.start;
    }

    public Color getColor() {
        return color;
    }

    public ImageIcon getIcon() {
        return eventType.getEventType().getEventIcon();
    }

    public void merge(JHVRelatedEvents found, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        eventsMap.get(eventType).remove(interval);
        eventsMap.get(eventType).remove(found.interval);

        interval.start = Math.min(interval.start, found.getStart());
        interval.end = Math.max(interval.end, found.getEnd());
        events.addAll(found.getEvents());
        associations.addAll(found.associations);
        eventsMap.get(eventType).put(interval, this);
    }

    public JHVEventType getJHVEventType() {
        return eventType;
    }

    public void highlight(boolean isHighlighted) {
        if (isHighlighted != highlighted) {
            highlighted = isHighlighted;
            fireHighlightChanged();
        }
    }

    public static void addHighlightListener(JHVEventHighlightListener l) {
        listeners.add(l);
    }

    public static void removeHighlightListener(JHVEventHighlightListener l) {
        listeners.remove(l);
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    private void fireHighlightChanged() {
        for (JHVEventHighlightListener l : listeners) {
            l.eventHightChanged(this);
        }
    }

    public JHVEvent getClosestTo(long timestamp) {
        for (JHVEvent event : events) {
            if (event.start <= timestamp && timestamp <= event.end) {
                return event;
            }
        }
        return events.get(0);
    }

    public void addAssociation(JHVAssociation association) {
        associations.add(association);
    }

    private JHVEvent findEvent(Integer id) {
        for (JHVEvent evt : events) {
            if (evt.getUniqueID().equals(id)) {
                return evt;
            }
        }
        return null;
    }

    public ArrayList<JHVEvent> getNextEvents(JHVEvent event) {
        ArrayList<JHVEvent> nEvents = new ArrayList<JHVEvent>();
        for (JHVAssociation assoc : associations) {
            if (assoc.left == event.getUniqueID()) {
                JHVEvent newEvt = findEvent(assoc.right);
                if (newEvt != null) {
                    nEvents.add(newEvt);
                }
            }
        }
        return nEvents;
    }

    public ArrayList<JHVEvent> getPreviousEvents(JHVEvent event) {
        ArrayList<JHVEvent> nEvents = new ArrayList<JHVEvent>();
        for (JHVAssociation assoc : associations) {
            if (assoc.right == event.getUniqueID()) {
                JHVEvent newEvt = findEvent(assoc.left);
                if (newEvt != null) {
                    nEvents.add(newEvt);
                }
            }
        }
        return nEvents;
    }

    public void swapEvent(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        int i = 0;
        while (!events.get(i).getUniqueID().equals(event.getUniqueID())) {
            i++;
        }
        events.remove(i);
        events.add(event);
        resetTime(eventsMap);
    }

    private void resetTime(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        eventsMap.get(eventType).remove(interval);
        interval.start = Long.MAX_VALUE;
        interval.end = Long.MIN_VALUE;
        for (JHVEvent evt : events) {
            long time = evt.start;
            if (time < interval.start) {
                interval.start = time;
            }
            time = evt.end;
            if (time > interval.end) {
                interval.end = time;
            }
        }
        eventsMap.get(eventType).put(interval, this);
    }

    public JHVEvent get(int uniqueID) {
        for (JHVEvent evt : events) {
            if (evt.getUniqueID() == uniqueID)
                return evt;
        }
        return null;
    }

}
