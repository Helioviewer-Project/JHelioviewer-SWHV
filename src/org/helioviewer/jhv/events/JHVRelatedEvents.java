package org.helioviewer.jhv.events;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.events.gui.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public class JHVRelatedEvents implements ClickableDrawable {

    private static final HashSet<JHVEventHighlightListener> listeners = new HashSet<>();

    private final ArrayList<JHVEvent> events = new ArrayList<>();
    private final SortedDateInterval interval = new SortedDateInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    private final ArrayList<JHVAssociation> associations = new ArrayList<>();

    private final SWEKSupplier supplier;
    private final Color color;
    private boolean highlighted;

    JHVRelatedEvents(JHVEvent event, Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        supplier = event.getSupplier();
        color = JHVCacheColors.getNextColor();
        highlighted = false;

        events.add(event);
        eventsMap.putIfAbsent(supplier, new TreeMap<>());

        interval.start = event.start;
        interval.end = event.end;
        eventsMap.get(supplier).put(interval, this);
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

    @Nullable
    public ImageIcon getIcon() {
        return supplier.getGroup().getIcon();
    }

    void merge(JHVRelatedEvents found, Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        events.addAll(found.events);
        associations.addAll(found.associations);

        eventsMap.putIfAbsent(supplier, new TreeMap<>());
        eventsMap.get(supplier).remove(interval);
        eventsMap.get(supplier).remove(found.interval);

        interval.start = Math.min(interval.start, found.interval.start);
        interval.end = Math.max(interval.end, found.interval.end);
        eventsMap.get(supplier).put(interval, this);
    }

    public SWEKSupplier getSupplier() {
        return supplier;
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

    @Nullable
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

    void swapEvent(JHVEvent event, Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        int eid = event.getUniqueID();
        int i = 0;
        while (events.get(i).getUniqueID() != eid) {
            i++;
        }
        events.remove(i);
        events.add(event);
        resetTime(eventsMap);
    }

    private void resetTime(Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
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

        eventsMap.putIfAbsent(supplier, new TreeMap<>());
        eventsMap.get(supplier).remove(interval);

        interval.start = start;
        interval.end = end;
        eventsMap.get(supplier).put(interval, this);
    }

    @Override
    public void clicked(Point locationOnScreen, long timestamp) {
        SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(this, getClosestTo(timestamp));
        dialog.pack();
        dialog.setLocation(locationOnScreen);
        dialog.setVisible(true);
    }

}
