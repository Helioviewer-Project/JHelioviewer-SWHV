package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.container.cache.JHVCacheColors;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;

public class JHVRelatedEvents {
    private final ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
    private final SortedDateInterval interval = new SortedDateInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    private final ArrayList<JHVAssociation> associations = new ArrayList<JHVAssociation>();

    private final Color color;

    protected boolean highlighted;
    protected final static Set<JHVEventHighlightListener> listeners = new HashSet<JHVEventHighlightListener>();
    private final JHVEventType eventType;

    public JHVRelatedEvents(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        super();
        color = JHVCacheColors.getNextColor();
        eventType = event.getJHVEventType();
        this.add(event, eventsMap);
        highlighted = false;
    }

    public ArrayList<JHVEvent> getEvents() {
        return events;
    }

    private void add(JHVEvent evt, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        eventsMap.get(eventType).remove(interval);
        if (evt.start < interval.start) {
            interval.start = evt.start;
        }

        if (evt.end > interval.end) {
            interval.end = evt.end;
        }
        events.add(evt);
        eventsMap.get(eventType).put(interval, this);
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
        eventsMap.get(found.getJHVEventType()).remove(found.getInterval());

        interval.start = Math.min(interval.start, found.getStart());
        interval.end = Math.max(interval.end, found.getEnd());
        events.addAll(found.getEvents());
        associations.addAll(found.getAssociations());
        eventsMap.get(eventType).put(interval, this);
    }

    public SortedDateInterval getInterval() {
        return interval;
    }

    public ArrayList<JHVAssociation> getAssociations() {
        return associations;
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
            if (event.start <= timestamp && event.end >= timestamp) {
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
            if (assoc.left.equals(event.getUniqueID())) {
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
            if (assoc.right.equals(event.getUniqueID())) {
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
}
