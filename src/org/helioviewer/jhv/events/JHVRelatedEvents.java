package org.helioviewer.jhv.events;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.events.info.SWEKEventInformationDialog;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public class JHVRelatedEvents implements ClickableDrawable {

    private static final Colors.Data eventColors = new Colors.Data();
    private static final ArrayList<JHVEventListener.Highlight> listeners = new ArrayList<>();

    private final ArrayList<JHVEvent> events = new ArrayList<>();
    private final List<Pair<Integer, Integer>> associations = new ArrayList<>();
    private final SWEKSupplier supplier;
    private final Color color;

    private Interval interval;
    private boolean highlighted;

    JHVRelatedEvents(JHVEvent event, Map<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> eventsMap) {
        this.supplier = event.getSupplier();
        this.color = eventColors.getNextColor();
        this.events.add(event);
        this.interval = new Interval(event.start, event.end);
        addToMap(eventsMap);
    }

    private void addToMap(Map<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> eventsMap) {
        eventsMap.computeIfAbsent(supplier, k -> new TreeMap<>())
                 .computeIfAbsent(interval.start, k -> new ArrayList<>())
                 .add(this);
    }

    private void removeFromMap(Map<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> eventsMap) {
        TreeMap<Long, List<JHVRelatedEvents>> supplierMap = eventsMap.get(supplier);
        if (supplierMap != null) {
            List<JHVRelatedEvents> list = supplierMap.get(interval.start);
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) supplierMap.remove(interval.start);
            }
        }
    }

    public List<JHVEvent> getEvents() { return events; }
    public long getEnd() { return interval.end; }
    public long getStart() { return interval.start; }
    public Color getColor() { return color; }
    public SWEKSupplier getSupplier() { return supplier; }
    public boolean isHighlighted() { return highlighted; }

    @Nonnull
    public ImageIcon getIcon() { return supplier.getGroup().getIcon(); }

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
    }

    public JHVEvent getClosestTo(long timestamp) {
        for (JHVEvent event : events) {
            if (event.start <= timestamp && timestamp <= event.end) return event;
        }
        return events.isEmpty() ? null : events.get(0);
    }

    public List<JHVEvent> getNextEvents(JHVEvent event) {
        List<JHVEvent> nEvents = new ArrayList<>();
        int id = event.getUniqueID();
        for (Pair<Integer, Integer> assoc : associations) {
            if (assoc.left() == id) {
                JHVEvent found = findEvent(assoc.right());
                if (found != null) nEvents.add(found);
            }
        }
        return nEvents;
    }

    public List<JHVEvent> getPreviousEvents(JHVEvent event) {
        List<JHVEvent> pEvents = new ArrayList<>();
        int id = event.getUniqueID();
        for (Pair<Integer, Integer> assoc : associations) {
            if (assoc.right() == id) {
                JHVEvent found = findEvent(assoc.left());
                if (found != null) pEvents.add(found);
            }
        }
        return pEvents;
    }

    @Nullable
    private JHVEvent findEvent(int id) {
        for (JHVEvent evt : events) if (evt.getUniqueID() == id) return evt;
        return null;
    }

    void addAssociation(Pair<Integer, Integer> association) {
        associations.add(association);
    }

    void swapEvent(JHVEvent event, Map<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> eventsMap) {
        removeFromMap(eventsMap);
        events.removeIf(e -> e.getUniqueID() == event.getUniqueID());
        events.add(event);
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        for (JHVEvent evt : events) {
            start = Math.min(start, evt.start);
            end = Math.max(end, evt.end);
        }
        interval = new Interval(start, end);
        addToMap(eventsMap);
    }

    void merge(JHVRelatedEvents found, Map<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> eventsMap) {
        removeFromMap(eventsMap);
        found.removeFromMap(eventsMap);
        events.addAll(found.events);
        associations.addAll(found.associations);
        interval = new Interval(Math.min(interval.start, found.interval.start), Math.max(interval.end, found.interval.end));
        addToMap(eventsMap);
    }

    @Override
    public void clicked(Point loc, long ts) {
        new SWEKEventInformationDialog(this, getClosestTo(ts)).setVisible(true);
    }
}
