package org.helioviewer.jhv.events;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.events.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public class JHVRelatedEvents implements ClickableDrawable {

    private static final ArrayList<JHVEventListener.Highlight> listeners = new ArrayList<>();

    private final ArrayList<JHVEvent> events = new ArrayList<>();
    private final List<Pair<Integer, Integer>> associations = new ArrayList<>();
    private final SWEKSupplier supplier;
    private final Color color;

    private Interval interval;
    private boolean highlighted;

    JHVRelatedEvents(JHVEvent event, Map<SWEKSupplier, SortedMap<Interval, JHVRelatedEvents>> eventsMap) {
        supplier = event.getSupplier();
        color = EventColors.getNextColor();
        highlighted = false;

        events.add(event);
        eventsMap.computeIfAbsent(supplier, k -> new TreeMap<>());

        interval = new Interval(event.start, event.end);
        eventsMap.get(supplier).put(interval, this);
    }

    public List<JHVEvent> getEvents() {
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

    @Nonnull
    public ImageIcon getIcon() {
        return supplier.getGroup().getIcon();
    }

    void merge(JHVRelatedEvents found, Map<SWEKSupplier, SortedMap<Interval, JHVRelatedEvents>> eventsMap) {
        events.addAll(found.events);
        associations.addAll(found.associations);

        eventsMap.computeIfAbsent(supplier, k -> new TreeMap<>());
        eventsMap.get(supplier).remove(found.interval);
        eventsMap.get(supplier).remove(interval);

        interval = new Interval(Math.min(interval.start, found.interval.start), Math.max(interval.end, found.interval.end));
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

    public static void addHighlightListener(JHVEventListener.Highlight listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeHighlightListener(JHVEventListener.Highlight listener) {
        listeners.remove(listener);
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    private static void fireHighlightChanged() {
        listeners.forEach(JHVEventListener.Highlight::highlightChanged);
    }

    public JHVEvent getClosestTo(long timestamp) {
        for (JHVEvent event : events) {
            if (event.start <= timestamp && timestamp <= event.end) {
                return event;
            }
        }
        return events.get(0);
    }

    void addAssociation(Pair<Integer, Integer> association) {
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

    public List<JHVEvent> getNextEvents(JHVEvent event) {
        List<JHVEvent> nEvents = new ArrayList<>();
        for (Pair<Integer, Integer> assoc : associations) {
            if (assoc.left() == event.getUniqueID()) {
                JHVEvent newEvt = findEvent(assoc.right());
                if (newEvt != null) {
                    nEvents.add(newEvt);
                }
            }
        }
        return nEvents;
    }

    public List<JHVEvent> getPreviousEvents(JHVEvent event) {
        List<JHVEvent> nEvents = new ArrayList<>();
        for (Pair<Integer, Integer> assoc : associations) {
            if (assoc.right() == event.getUniqueID()) {
                JHVEvent newEvt = findEvent(assoc.left());
                if (newEvt != null) {
                    nEvents.add(newEvt);
                }
            }
        }
        return nEvents;
    }

    void swapEvent(JHVEvent event, Map<SWEKSupplier, SortedMap<Interval, JHVRelatedEvents>> eventsMap) {
        int eid = event.getUniqueID();
        int i = 0;
        while (events.get(i).getUniqueID() != eid) {
            i++;
        }
        events.remove(i);
        events.add(event);
        resetTime(eventsMap);
    }

    private void resetTime(Map<SWEKSupplier, SortedMap<Interval, JHVRelatedEvents>> eventsMap) {
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

        eventsMap.computeIfAbsent(supplier, k -> new TreeMap<>()).remove(interval);

        interval = new Interval(start, end);
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
