package org.helioviewer.jhv.data.cache;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.event.JHVAssociation;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.JHVEventHighlightListener;
import org.helioviewer.jhv.data.event.JHVEventType;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public class JHVRelatedEvents implements ClickableDrawable {

    private static final HashSet<JHVEventHighlightListener> listeners = new HashSet<>();

    private final ArrayList<JHVEvent> events = new ArrayList<>();
    private final SortedDateInterval interval = new SortedDateInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    private final ArrayList<JHVAssociation> associations = new ArrayList<>();

    private final JHVEventType eventType;
    private final Color color;
    private boolean highlighted;

    JHVRelatedEvents(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        eventType = event.getJHVEventType();
        color = JHVCacheColors.getNextColor();
        highlighted = false;

        events.add(event);

        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<>());
        }

        interval.start = event.start;
        interval.end = event.end;
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

    void merge(JHVRelatedEvents found, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        events.addAll(found.events);
        associations.addAll(found.associations);

        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<>());
        }
        eventsMap.get(eventType).remove(interval);
        eventsMap.get(eventType).remove(found.interval);

        interval.start = Math.min(interval.start, found.interval.start);
        interval.end = Math.max(interval.end, found.interval.end);
        eventsMap.get(eventType).put(interval, this);
    }

    public JHVEventType getJHVEventType() {
        return eventType;
    }

    void highlight(boolean isHighlighted) {
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

    private static void fireHighlightChanged() {
        for (JHVEventHighlightListener l : listeners) {
            l.eventHightChanged();
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

    void addAssociation(JHVAssociation association) {
        associations.add(association);
    }

    private JHVEvent findEvent(int id) {
        for (JHVEvent evt : events) {
            if (evt.getUniqueID() == id) {
                return evt;
            }
        }
        return null;
    }

    public ArrayList<JHVEvent> getNextEvents(JHVEvent event) {
        ArrayList<JHVEvent> nEvents = new ArrayList<>();
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
        ArrayList<JHVEvent> nEvents = new ArrayList<>();
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

    void swapEvent(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        int i = 0;
        while (events.get(i).getUniqueID() != event.getUniqueID()) {
            i++;
        }
        events.remove(i);
        events.add(event);
        resetTime(eventsMap);
    }

    private void resetTime(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        for (JHVEvent evt : events) {
            long time = evt.start;
            if (time < start) {
                start = time;
            }
            time = evt.end;
            if (time > end) {
                end = time;
            }
        }

        if (!eventsMap.containsKey(eventType)) {
            eventsMap.put(eventType, new TreeMap<>());
        }
        eventsMap.get(eventType).remove(interval);

        interval.start = start;
        interval.end = end;
        eventsMap.get(eventType).put(interval, this);
    }

    @Override
    public void clicked(Point locationOnScreen, long timestamp) {
        SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(this, getClosestTo(timestamp));
        dialog.pack();
        dialog.setLocation(locationOnScreen);
        dialog.setVisible(true);
    }

}
