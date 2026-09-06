package org.helioviewer.jhv.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.time.Interval;

public class ObservationGroup {

    private static final Colors.Data eventColors = new Colors.Data();

    private final LinkedHashMap<Integer, SolarEvent> events = new LinkedHashMap<>();
    private final LinkedHashSet<SolarEvent.Link> associations = new LinkedHashSet<>();
    private final Color color;

    private List<Interval> intervals;
    private boolean highlighted;

    public ObservationGroup(SolarEvent event) {
        this(event, eventColors.getNextColor());
    }

    public ObservationGroup(SolarEvent event, Color _color) {
        color = _color;
        events.put(event.getUniqueID(), event);
        intervals = List.of(new Interval(event.start, event.end));
    }

    Collection<SolarEvent> getEvents() {
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

    public SolarEvent getClosestTo(long timestamp) {
        SolarEvent closest = events.sequencedValues().getFirst();
        long minimumDistance = Long.MAX_VALUE;
        for (SolarEvent event : events.values()) {
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

    public List<SolarEvent> getAssociatedEvents(SolarEvent event) {
        int id = event.getUniqueID();
        List<SolarEvent> result = new ArrayList<>();
        for (SolarEvent.Link link : associations) {
            int target;
            if (link.firstId() == id)
                target = link.secondId();
            else if (link.secondId() == id)
                target = link.firstId();
            else
                continue;

            SolarEvent found = events.get(target);
            if (found != null)
                result.add(found);
        }
        return result;
    }

    void addAssociation(SolarEvent.Link link) {
        associations.add(link);
    }

    Collection<SolarEvent.Link> getAssociations() {
        return associations;
    }

    void swapEvent(SolarEvent event) {
        SolarEvent previous = events.putLast(event.getUniqueID(), event);
        if (previous != null && previous.start == event.start && previous.end == event.end)
            return;

        List<Interval> updated = new ArrayList<>();
        for (SolarEvent observation : events.values())
            updated.add(new Interval(observation.start, observation.end));
        intervals = List.copyOf(Interval.merge(updated));
    }

    void merge(ObservationGroup found) {
        events.putAll(found.events);
        associations.addAll(found.associations);
        List<Interval> combined = new ArrayList<>(intervals);
        combined.addAll(found.intervals);
        intervals = List.copyOf(Interval.merge(combined));
    }

}
