package org.helioviewer.jhv.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.time.Interval;

public class JHVRelatedEvents {

    private static final Colors.Data eventColors = new Colors.Data();
    private static final ArrayList<JHVEventListener.Highlight> listeners = new ArrayList<>();

    private final ArrayList<JHVEvent> events = new ArrayList<>();
    private final HashMap<Integer, JHVEvent> eventsById = new HashMap<>();
    private final List<JHVEvent.Link> associations = new ArrayList<>();
    private final SWEKSupplier supplier;
    private final Color color;

    private Interval interval;
    private boolean highlighted;

    JHVRelatedEvents(JHVEvent event) {
        supplier = event.getSupplier();
        color = eventColors.getNextColor();
        addEvent(event);
        interval = new Interval(event.start, event.end);
    }

    public List<JHVEvent> getEvents() {
        return events;
    }

    public long getEnd() {
        return interval.end();
    }

    public long getStart() {
        return interval.start();
    }

    public Color getColor() {
        return color;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    @Nonnull
    public SWEKGroup getGroup() {
        return supplier.getGroup();
    }

    void highlight(boolean isHighlighted) {
        if (isHighlighted != highlighted) {
            highlighted = isHighlighted;
            fireHighlightChanged();
        }
    }

    public static void addHighlightListener(JHVEventListener.Highlight listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static void removeHighlightListener(JHVEventListener.Highlight listener) {
        listeners.remove(listener);
    }

    private static void fireHighlightChanged() {
        listeners.forEach(JHVEventListener.Highlight::highlightChanged);
        DisplayController.display();
    }

    public JHVEvent getClosestTo(long timestamp) {
        for (JHVEvent event : events) {
            if (event.start <= timestamp && timestamp <= event.end) return event;
        }
        return events.isEmpty() ? null : events.getFirst();
    }

    public List<JHVEvent> getNextEvents(JHVEvent event) {
        return getAssociatedEvents(event.getUniqueID(), true);
    }

    public List<JHVEvent> getPreviousEvents(JHVEvent event) {
        return getAssociatedEvents(event.getUniqueID(), false);
    }

    private List<JHVEvent> getAssociatedEvents(int id, boolean next) {
        List<JHVEvent> result = new ArrayList<>();
        for (JHVEvent.Link link : associations) {
            int source = next ? link.leftId() : link.rightId();
            if (source == id) {
                int target = next ? link.rightId() : link.leftId();
                JHVEvent found = eventsById.get(target);
                if (found != null)
                    result.add(found);
            }
        }
        return result;
    }

    void addAssociation(JHVEvent.Link link) {
        associations.add(link);
    }

    private void addEvent(JHVEvent event) {
        events.add(event);
        eventsById.put(event.getUniqueID(), event);
    }

    void swapEvent(JHVEvent event) {
        events.removeIf(e -> e.getUniqueID() == event.getUniqueID());
        eventsById.put(event.getUniqueID(), event);
        events.add(event);
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        for (JHVEvent evt : events) {
            start = Math.min(start, evt.start);
            end = Math.max(end, evt.end);
        }
        interval = new Interval(start, end);
    }

    void merge(JHVRelatedEvents found) {
        events.addAll(found.events);
        eventsById.putAll(found.eventsById);
        associations.addAll(found.associations);
        interval = new Interval(Math.min(interval.start(), found.interval.start()), Math.max(interval.end(), found.interval.end()));
    }

}
