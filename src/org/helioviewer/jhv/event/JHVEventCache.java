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

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

public class JHVEventCache {

    private static final double FACTOR = 0.2;
    private static final long MAX_EVENT_DURATION = TimeUtils.DAY_IN_MILLIS * 14;

    private static final Set<JHVEventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final Set<JHVEventListener.Highlight> highlightListeners = new HashSet<>();
    private static final Map<SWEKSupplier, NavigableMap<Long, List<JHVRelatedEvents>>> events = new HashMap<>();
    private static final Map<Integer, JHVRelatedEvents> relatedEventsById = new HashMap<>();
    private static final Set<SWEKSupplier> activeEventTypes = new HashSet<>();
    private static final Map<SWEKSupplier, RequestCache> downloadedCache = new HashMap<>();
    private static final Map<Integer, Set<JHVEvent.Link>> pendingAssocs = new HashMap<>();

    private static JHVRelatedEvents lastHighlighted = null;

    public static void registerHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.add(handler);
    }

    public static void requestForInterval(long start, long end, JHVEventListener.Handle handler) {
        downloadMissingIntervals(start, end);
        handler.newEventsReceived();
    }

    public static void unregisterHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    static void fireEventCacheChanged() {
        cacheEventHandlers.forEach(JHVEventListener.Handle::cacheUpdated);
    }

    static void intervalNotDownloaded(SWEKSupplier eventType, long start, long end) {
        RequestCache cache = downloadedCache.get(eventType);
        if (cache != null)
            cache.removeRequestedInterval(start, end);
    }

    public static boolean isSupplierActive(SWEKSupplier supplier) {
        return activeEventTypes.contains(supplier);
    }

    public static void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (active) {
            activeEventTypes.add(supplier);
            downloadedCache.computeIfAbsent(supplier, k -> new RequestCache());
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

    public static void addEvent(JHVEvent event) {
        Integer id = event.getUniqueID();
        JHVRelatedEvents relatedEvents = relatedEventsById.get(id);
        if (relatedEvents != null) {
            removeFromIndex(relatedEvents);
            relatedEvents.swapEvent(event);
            addToIndex(relatedEvents);
        } else {
            createNewRelatedEvent(event);
        }
        resolvePendingAssociations(id);
    }

    public static JHVRelatedEvents getRelatedEvents(int id) {
        return relatedEventsById.get(id);
    }

    private static void resolvePendingAssociations(Integer id) {
        Set<JHVEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(JHVEventCache::addAssociation);
    }

    private static void createNewRelatedEvent(JHVEvent event) {
        JHVRelatedEvents revent = new JHVRelatedEvents(event);
        addToIndex(revent);
        relatedEventsById.put(event.getUniqueID(), revent);
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
        events.computeIfAbsent(event.getSupplier(), _ -> new TreeMap<>())
                .computeIfAbsent(event.getStart(), _ -> new ArrayList<>())
                .add(event);
    }

    private static void removeFromIndex(JHVRelatedEvents event) {
        NavigableMap<Long, List<JHVRelatedEvents>> supplierEvents = events.get(event.getSupplier());
        if (supplierEvents == null)
            return;

        List<JHVRelatedEvents> list = supplierEvents.get(event.getStart());
        if (list == null)
            return;

        list.remove(event);
        if (list.isEmpty())
            supplierEvents.remove(event.getStart());
    }

    static void addAssociation(JHVEvent.Link link) {
        JHVRelatedEvents left = relatedEventsById.get(link.leftId());
        JHVRelatedEvents right = relatedEventsById.get(link.rightId());
        if (left != null && right != null) {
            if (left != right) {
                merge(left, right);
                left.addAssociation(link);
            }
        } else {
            if (left == null)
                addPendingAssociation(link.leftId(), link);
            if (right == null)
                addPendingAssociation(link.rightId(), link);
        }
    }

    private static void addPendingAssociation(int id, JHVEvent.Link link) {
        pendingAssocs.computeIfAbsent(id, k -> new HashSet<>()).add(link);
    }

    public static List<JHVRelatedEvents> getEvents(long start, long end) {
        if (activeEventTypes.isEmpty()) return Collections.emptyList();
        List<JHVRelatedEvents> result = new ArrayList<>();
        for (SWEKSupplier evt : activeEventTypes) {
            NavigableMap<Long, List<JHVRelatedEvents>> supplierMap = events.get(evt);
            if (supplierMap != null) {
                // Find all events that can overlap the requested range.
                NavigableMap<Long, List<JHVRelatedEvents>> relevantRange =
                        supplierMap.subMap(start - MAX_EVENT_DURATION, true, end, true);

                for (List<JHVRelatedEvents> list : relevantRange.values()) {
                    for (JHVRelatedEvents event : list) {
                        if (event.getStart() <= end && event.getEnd() >= start) {
                            result.add(event);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void downloadMissingIntervals(long start, long end) {
        long deltaT = Math.max((long) ((end - start) * FACTOR), TimeUtils.DAY_IN_MILLIS);
        for (SWEKSupplier supplier : activeEventTypes) {
            RequestCache rc = downloadedCache.get(supplier);
            if (rc != null) {
                List<Interval> missing = rc.getMissingIntervals(start, end);
                if (!missing.isEmpty()) {
                    SWEKDownloader.startDownloadSupplier(supplier, rc.adaptRequestCache(start - deltaT, end + deltaT));
                }
            }
        }
    }

    static void removeSupplier(SWEKSupplier supplier, boolean keepActive) {
        downloadedCache.put(supplier, new RequestCache());
        events.remove(supplier);
        relatedEventsById.entrySet().removeIf(entry -> removeEventIfFromSupplier(entry, supplier));
        if (!keepActive) activeEventTypes.remove(supplier);
        fireEventCacheChanged();
    }

    private static boolean removeEventIfFromSupplier(Map.Entry<Integer, JHVRelatedEvents> entry, SWEKSupplier supplier) {
        if (entry.getValue().getSupplier() != supplier)
            return false;
        pendingAssocs.remove(entry.getKey());
        return true;
    }

    public static List<Interval> getAllRequestIntervals(SWEKSupplier eventType) {
        RequestCache rc = downloadedCache.get(eventType);
        return (rc != null) ? rc.getAllRequestIntervals() : Collections.emptyList();
    }
}
