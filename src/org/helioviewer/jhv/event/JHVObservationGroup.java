package org.helioviewer.jhv.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.time.Interval;

public class JHVObservationGroup {

    private static final Colors.Data eventColors = new Colors.Data();

    private final LinkedHashMap<Integer, JHVEvent> events = new LinkedHashMap<>();
    private final LinkedHashSet<JHVEvent.Link> associations = new LinkedHashSet<>();
    private final Color color;

    private List<Interval> intervals;
    private boolean highlighted;

    public JHVObservationGroup(JHVEvent event) {
        this(event, eventColors.getNextColor());
    }

    public JHVObservationGroup(JHVEvent event, Color _color) {
        color = _color;
        events.put(event.getUniqueID(), event);
        intervals = List.of(new Interval(event.start, event.end));
    }

    Collection<JHVEvent> getEvents() {
        return events.values();
    }

    public long getEnd() {
        return intervals.getLast().end();
    }

    public long getStart() {
        return intervals.getFirst().start();
    }

    public Color getColor() {
        return color;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    boolean highlight(boolean isHighlighted) {
        if (isHighlighted == highlighted)
            return false;
        highlighted = isHighlighted;
        return true;
    }

    public JHVEvent getClosestTo(long timestamp) {
        JHVEvent closest = events.sequencedValues().getFirst();
        long minimumDistance = Long.MAX_VALUE;
        for (JHVEvent event : events.values()) {
            if (event.start <= timestamp && timestamp <= event.end) return event;
            long distance = timestamp < event.start ? event.start - timestamp : timestamp - event.end;
            if (distance < minimumDistance) {
                minimumDistance = distance;
                closest = event;
            }
        }
        return closest;
    }

    boolean overlaps(long start, long end) {
        for (Interval interval : intervals) {
            if (interval.start() > end)
                return false;
            if (interval.end() >= start)
                return true;
        }
        return false;
    }

    public List<Interval> getIntervals() {
        return intervals;
    }

    // Union of actual event intervals, preserving gaps in an associated group.
    private void updateIntervals() {
        List<Interval> sorted = new ArrayList<>();
        for (JHVEvent event : events.values())
            sorted.add(new Interval(event.start, event.end));
        sorted.sort(null);
        List<Interval> result = new ArrayList<>();
        for (Interval next : sorted) {
            if (result.isEmpty() || result.getLast().end() < next.start()) {
                result.add(next);
            } else {
                Interval previous = result.removeLast();
                result.add(new Interval(previous.start(), Math.max(previous.end(), next.end())));
            }
        }
        intervals = List.copyOf(result);
    }

    public List<JHVEvent> getAssociatedEvents(JHVEvent event) {
        int id = event.getUniqueID();
        List<JHVEvent> result = new ArrayList<>();
        for (JHVEvent.Link link : associations) {
            int target;
            if (link.firstId() == id)
                target = link.secondId();
            else if (link.secondId() == id)
                target = link.firstId();
            else
                continue;

            JHVEvent found = events.get(target);
            if (found != null)
                result.add(found);
        }
        return result;
    }

    void addAssociation(JHVEvent.Link link) {
        associations.add(link);
    }

    Collection<JHVEvent.Link> getAssociations() {
        return associations;
    }

    void swapEvent(JHVEvent event) {
        JHVEvent previous = events.putLast(event.getUniqueID(), event);
        if (previous != null && previous.start == event.start && previous.end == event.end)
            return;

        updateIntervals();
    }

    void merge(JHVObservationGroup found) {
        events.putAll(found.events);
        associations.addAll(found.associations);
        updateIntervals();
    }

}
