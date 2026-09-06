package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

public class JHVEventCache {

    private static final double FACTOR = 0.2;
    private static final long REQUEST_REFRESH_INTERVAL = 60 * 60 * 1000L;
    private static final long FUTURE_REQUEST_MARGIN = 6 * REQUEST_REFRESH_INTERVAL;

    private static final Set<JHVEventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final Set<JHVEventListener.Highlight> highlightListeners = new HashSet<>();
    private static final NavigableMap<Long, List<JHVRelatedEvents>> events = new TreeMap<>();
    private static final Map<Integer, JHVRelatedEvents> relatedEventsById = new HashMap<>();
    private static final Map<SWEKSupplier, RequestCache> requestedIntervals = new HashMap<>();
    private static final Map<Integer, Set<JHVEvent.Link>> pendingAssocs = new HashMap<>();

    private static JHVRelatedEvents lastHighlighted = null;
    private static long maximumGroupDuration;

    public static void registerHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.add(handler);
    }

    public static void unregisterHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    static void fireEventCacheChanged() {
        cacheEventHandlers.forEach(JHVEventListener.Handle::cacheUpdated);
    }

    static void requestFailed(SWEKSupplier eventType, long start, long end) {
        RequestCache cache = requestedIntervals.get(eventType);
        if (cache != null)
            cache.removeRequestedInterval(start, end);
    }

    public static boolean isSupplierActive(SWEKSupplier supplier) {
        return requestedIntervals.containsKey(supplier);
    }

    public static void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (active) {
            requestedIntervals.computeIfAbsent(supplier, _ -> new RequestCache());
            fireEventCacheChanged();
        } else {
            SWEKDownloader.stopDownloadSupplier(supplier, false);
        }
    }

    public static void highlight(JHVRelatedEvents event) {
        if (event == lastHighlighted) return;
        boolean changed = false;
        if (event != null)
            changed = event.highlight(true);
        if (lastHighlighted != null)
            changed = lastHighlighted.highlight(false) || changed;
        lastHighlighted = event;
        if (changed)
            fireHighlightChanged();
    }

    public static void addHighlightListener(JHVEventListener.Highlight listener) {
        highlightListeners.add(listener);
    }

    public static void removeHighlightListener(JHVEventListener.Highlight listener) {
        highlightListeners.remove(listener);
    }

    private static void fireHighlightChanged() {
        highlightListeners.forEach(JHVEventListener.Highlight::highlightChanged);
        DisplayController.display();
    }

    static void addEvent(JHVEvent event) {
        Integer id = event.getUniqueID();
        JHVRelatedEvents relatedEvents = relatedEventsById.get(id);
        if (relatedEvents != null) {
            removeFromIndex(relatedEvents);
            relatedEvents.swapEvent(event);
            addToIndex(relatedEvents);
        } else {
            addNewRelatedEvent(event);
        }
        resolvePendingAssociations(id);
    }

    @Nullable
    public static JHVRelatedEvents getRelatedEvents(int id) {
        return relatedEventsById.get(id);
    }

    private static void resolvePendingAssociations(Integer id) {
        Set<JHVEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(JHVEventCache::addAssociation);
    }

    private static void addNewRelatedEvent(JHVEvent event) {
        addNewRelatedEvent(event, new JHVRelatedEvents(event));
    }

    private static void addNewRelatedEvent(JHVEvent event, JHVRelatedEvents relatedEvents) {
        addToIndex(relatedEvents);
        relatedEventsById.put(event.getUniqueID(), relatedEvents);
    }

    private static void merge(JHVRelatedEvents current, JHVRelatedEvents found) {
        if (current == found) return;
        removeFromIndex(current);
        removeFromIndex(found);
        current.merge(found);
        addToIndex(current);
        for (JHVEvent foundev : found.getEvents()) {
            relatedEventsById.put(foundev.getUniqueID(), current);
        }
    }

    private static void addToIndex(JHVRelatedEvents event) {
        events.computeIfAbsent(event.getStart(), _ -> new ArrayList<>()).add(event);
        maximumGroupDuration = Math.max(maximumGroupDuration, event.getEnd() - event.getStart());
    }

    private static void removeFromIndex(JHVRelatedEvents event) {
        List<JHVRelatedEvents> list = events.get(event.getStart());
        if (list == null)
            return;

        list.remove(event);
        if (list.isEmpty())
            events.remove(event.getStart());
    }

    static void addAssociation(JHVEvent.Link link) {
        JHVRelatedEvents first = relatedEventsById.get(link.firstId());
        JHVRelatedEvents second = relatedEventsById.get(link.secondId());
        if (first != null && second != null) {
            if (first != second)
                merge(first, second);
            first.addAssociation(link);
        } else {
            if (first == null)
                addPendingAssociation(link.firstId(), link);
            if (second == null)
                addPendingAssociation(link.secondId(), link);
        }
    }

    private static void addPendingAssociation(int id, JHVEvent.Link link) {
        pendingAssocs.computeIfAbsent(id, k -> new HashSet<>()).add(link);
    }

    public static List<JHVRelatedEvents> getEvents(long start, long end) {
        if (events.isEmpty()) return Collections.emptyList();
        List<JHVRelatedEvents> result = new ArrayList<>();
        NavigableMap<Long, List<JHVRelatedEvents>> relevantRange =
                events.subMap(start - maximumGroupDuration, true, end, true);
        for (List<JHVRelatedEvents> list : relevantRange.values()) {
            for (JHVRelatedEvents event : list) {
                if (event.getEnd() >= start && event.overlaps(start, end))
                    result.add(event);
            }
        }
        return result;
    }

    public static void requestForInterval(long start, long end) {
        long now = System.currentTimeMillis();
        long requestHorizon = now / REQUEST_REFRESH_INTERVAL * REQUEST_REFRESH_INTERVAL + FUTURE_REQUEST_MARGIN;
        long visibleEnd = Math.min(end, requestHorizon);
        if (start >= visibleEnd)
            return;

        long deltaT = Math.max((long) ((visibleEnd - start) * FACTOR), TimeUtils.DAY_IN_MILLIS);
        long requestStart = start - deltaT;
        long requestEnd = Math.min(visibleEnd + deltaT, requestHorizon);
        for (Map.Entry<SWEKSupplier, RequestCache> entry : requestedIntervals.entrySet()) {
            RequestCache cache = entry.getValue();
            if (!cache.getMissingIntervals(start, visibleEnd).isEmpty())
                SWEKDownloader.startDownloadSupplier(entry.getKey(), cache.adaptRequestCache(requestStart, requestEnd));
        }
    }

    static void removeSupplier(SWEKSupplier supplier, boolean keepActive) {
        if (keepActive)
            requestedIntervals.put(supplier, new RequestCache());
        else
            requestedIntervals.remove(supplier);
        removeSupplierEvents(supplier);
        fireEventCacheChanged();
    }

    private static void removeSupplierEvents(SWEKSupplier supplier) {
        Set<JHVRelatedEvents> affectedGroups = new HashSet<>();
        Set<Integer> removedIds = new HashSet<>();
        for (List<JHVRelatedEvents> groups : events.values()) {
            for (JHVRelatedEvents group : groups) {
                for (JHVEvent event : group.getEvents()) {
                    if (event.getSupplier() == supplier) {
                        affectedGroups.add(group);
                        removedIds.add(event.getUniqueID());
                    }
                }
            }
        }

        if (affectedGroups.isEmpty())
            return;

        pendingAssocs.values().forEach(links -> links.removeIf(link ->
                removedIds.contains(link.firstId()) || removedIds.contains(link.secondId())));
        pendingAssocs.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (affectedGroups.contains(lastHighlighted))
            highlight(null);

        for (JHVRelatedEvents group : affectedGroups)
            rebuildWithout(group, supplier);

        maximumGroupDuration = 0;
        for (List<JHVRelatedEvents> groups : events.values()) {
            for (JHVRelatedEvents group : groups)
                maximumGroupDuration = Math.max(maximumGroupDuration, group.getEnd() - group.getStart());
        }
    }

    private static void rebuildWithout(JHVRelatedEvents group, SWEKSupplier supplier) {
        removeFromIndex(group);
        for (JHVEvent event : group.getEvents())
            relatedEventsById.remove(event.getUniqueID());

        for (JHVEvent event : group.getEvents()) {
            if (event.getSupplier() != supplier)
                addNewRelatedEvent(event, new JHVRelatedEvents(event, group.getColor()));
        }
        for (JHVEvent.Link link : group.getAssociations()) {
            if (relatedEventsById.containsKey(link.firstId()) && relatedEventsById.containsKey(link.secondId()))
                addAssociation(link);
        }
    }

}
