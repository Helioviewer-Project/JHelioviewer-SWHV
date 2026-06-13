package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

public class JHVEventCache {

    private static final double FACTOR = 0.2;
    private static final long MAX_EVENT_DURATION = TimeUtils.DAY_IN_MILLIS * 14;

    private static final HashSet<JHVEventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final HashMap<SWEKSupplier, TreeMap<Long, List<JHVRelatedEvents>>> events = new HashMap<>();
    private static final HashMap<Integer, JHVRelatedEvents> relEvents = new HashMap<>();
    private static final HashSet<SWEKSupplier> activeEventTypes = new HashSet<>();
    private static final HashMap<SWEKSupplier, RequestCache> downloadedCache = new HashMap<>();
    private static final HashMap<Integer, HashSet<JHVEvent.Link>> pendingAssocs = new HashMap<>();

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
        downloadedCache.get(eventType).removeRequestedInterval(start, end);
    }

    static void supplierActivated(SWEKSupplier supplier) {
        activeEventTypes.add(supplier);
        downloadedCache.computeIfAbsent(supplier, k -> new RequestCache());
        fireEventCacheChanged();
    }

    public static void highlight(JHVRelatedEvents event) {
        if (event == lastHighlighted) return;
        if (event != null) event.highlight(true);
        if (lastHighlighted != null) lastHighlighted.highlight(false);
        lastHighlighted = event;
    }

    public static void addEvent(JHVEvent event) {
        Integer id = event.getUniqueID();
        JHVRelatedEvents relatedEvents = relEvents.get(id);
        if (relatedEvents != null) {
            relatedEvents.swapEvent(event, events);
        } else {
            createNewRelatedEvent(event);
        }
        resolvePendingAssociations(id);
    }

    public static JHVRelatedEvents getRelatedEvents(int id) {
        return relEvents.get(id);
    }

    private static void resolvePendingAssociations(Integer id) {
        HashSet<JHVEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(JHVEventCache::addAssociation);
    }

    private static void createNewRelatedEvent(JHVEvent event) {
        JHVRelatedEvents revent = new JHVRelatedEvents(event, events);
        relEvents.put(event.getUniqueID(), revent);
    }

    private static void merge(JHVRelatedEvents current, JHVRelatedEvents found) {
        if (current == found) return;
        current.merge(found, events);
        for (JHVEvent foundev : found.getEvents()) {
            relEvents.put(foundev.getUniqueID(), current);
        }
    }

    static void addAssociation(JHVEvent.Link link) {
        JHVRelatedEvents left = relEvents.get(link.leftId());
        JHVRelatedEvents right = relEvents.get(link.rightId());
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
            TreeMap<Long, List<JHVRelatedEvents>> supplierMap = events.get(evt);
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
        relEvents.entrySet().removeIf(entry -> removeEventIfFromSupplier(entry, supplier));
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
