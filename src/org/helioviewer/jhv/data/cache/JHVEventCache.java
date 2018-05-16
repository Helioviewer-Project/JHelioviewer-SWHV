package org.helioviewer.jhv.data.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.event.JHVAssociation;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.SWEKDownloadManager;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.time.TimeUtils;

public class JHVEventCache {

    private static final double factor = 0.2;

    private static final HashSet<JHVEventHandler> cacheEventHandlers = new HashSet<>();
    private static final HashMap<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = new HashMap<>();
    private static final HashMap<Integer, JHVRelatedEvents> relEvents = new HashMap<>();
    private static final HashSet<SWEKSupplier> activeEventTypes = new HashSet<>();
    private static final HashMap<SWEKSupplier, RequestCache> downloadedCache = new HashMap<>();
    private static final ArrayList<JHVAssociation> assocs = new ArrayList<>();

    private static JHVRelatedEvents lastHighlighted = null;

    public static void requestForInterval(long startDate, long endDate, JHVEventHandler handler) {
        long deltaT = Math.max((long) ((endDate - startDate) * factor), TimeUtils.DAY_IN_MILLIS);
        long newStartDate = startDate - deltaT;
        long newEndDate = endDate + deltaT;

        cacheEventHandlers.add(handler);

        Map<SWEKSupplier, List<Interval>> missingIntervals = get(startDate, endDate, newStartDate, newEndDate).getMissingIntervals();
        for (Map.Entry<SWEKSupplier, List<Interval>> entry : missingIntervals.entrySet()) {
            SWEKSupplier eventType = entry.getKey();
            for (Interval missing : entry.getValue()) {
                SWEKDownloadManager.startDownloadSupplier(eventType, missing);
            }
        }
        handler.newEventsReceived();
    }

    public static void fireEventCacheChanged() {
        for (JHVEventHandler handler : cacheEventHandlers) {
            handler.cacheUpdated();
        }
    }

    public static void intervalsNotDownloaded(SWEKSupplier eventType, Interval interval) {
        downloadedCache.get(eventType).removeRequestedInterval(interval.start, interval.end);
        get(interval.start, interval.end, interval.start, interval.end);
    }

    public static void supplierActivated(SWEKSupplier supplier) {
        activeEventTypes.add(supplier);
        downloadedCache.putIfAbsent(supplier, new RequestCache());
        fireEventCacheChanged();
    }

    public static void highlight(JHVRelatedEvents event) {
        if (event == lastHighlighted) {
            return;
        }
        if (event != null) {
            event.highlight(true);
        }
        if (lastHighlighted != null) {
            lastHighlighted.highlight(false);
        }
        lastHighlighted = event;
    }

    public static void add(JHVEvent event) {
        Integer id = event.getUniqueID();
        if (relEvents.containsKey(id)) {
            relEvents.get(id).swapEvent(event, events);
        } else {
            createNewRelatedEvent(event);
        }
        checkAssociation(event);
    }

    public static JHVRelatedEvents getRelatedEvents(int id) {
        return relEvents.get(id);
    }

    private static void checkAssociation(JHVEvent event) {
        int uid = event.getUniqueID();
        JHVRelatedEvents rEvent = relEvents.get(uid);
        for (Iterator<JHVAssociation> iterator = assocs.iterator(); iterator.hasNext();) {
            JHVAssociation tocheck = iterator.next();
            if (tocheck.left == uid && relEvents.containsKey(tocheck.right)) {
                merge(rEvent, relEvents.get(tocheck.right));
                rEvent.addAssociation(tocheck);
                iterator.remove();
            }
            if (tocheck.right == uid && relEvents.containsKey(tocheck.left)) {
                merge(rEvent, relEvents.get(tocheck.left));
                rEvent.addAssociation(tocheck);
                iterator.remove();
            }
        }
    }

    private static void createNewRelatedEvent(JHVEvent event) {
        JHVRelatedEvents revent = new JHVRelatedEvents(event, events);
        relEvents.put(event.getUniqueID(), revent);
    }

    private static void merge(JHVRelatedEvents current, JHVRelatedEvents found) {
        if (current == found) {
            return;
        }
        current.merge(found, events);
        for (JHVEvent foundev : found.getEvents()) {
            Integer key = foundev.getUniqueID();
            relEvents.remove(key);
            relEvents.put(key, current);
        }
    }

    public static void add(JHVAssociation association) {
        if (relEvents.containsKey(association.left) && relEvents.containsKey(association.right)) {
            JHVRelatedEvents ll = relEvents.get(association.left);
            JHVRelatedEvents rr = relEvents.get(association.right);
            if (ll != rr) {
                merge(ll, rr);
                ll.addAssociation(association);
            }
        } else {
            assocs.add(association);
        }
    }

    public static JHVEventCacheResult get(long startDate, long endDate) {
        return get(startDate, endDate, startDate, endDate);
    }

    private static JHVEventCacheResult get(long startDate, long endDate, long extendedStart, long extendedEnd) {
        HashMap<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsResult = new HashMap<>();
        HashMap<SWEKSupplier, List<Interval>> missingIntervals = new HashMap<>();

        for (SWEKSupplier evt : activeEventTypes) {
            SortedMap<SortedDateInterval, JHVRelatedEvents> sortedEvents = events.get(evt);
            if (sortedEvents != null) {
                long delta = TimeUtils.DAY_IN_MILLIS * 30L;
                SortedMap<SortedDateInterval, JHVRelatedEvents> submap = sortedEvents.subMap(new SortedDateInterval(startDate - delta, startDate - delta), new SortedDateInterval(endDate + delta, endDate + delta));
                eventsResult.put(evt, submap);
            }
            List<Interval> missing = downloadedCache.get(evt).getMissingIntervals(startDate, endDate);
            if (!missing.isEmpty()) {
                missing = downloadedCache.get(evt).adaptRequestCache(extendedStart, extendedEnd);
                missingIntervals.put(evt, missing);
            }
        }
        return new JHVEventCacheResult(eventsResult, missingIntervals);
    }

    public static void removeSupplier(SWEKSupplier supplier, boolean keepActive) {
        downloadedCache.put(supplier, new RequestCache());
        events.remove(supplier);
        relEvents.entrySet().removeIf(entry -> entry.getValue().getSupplier() == supplier);

        if (!keepActive)
            activeEventTypes.remove(supplier);
        fireEventCacheChanged();
    }

    public static List<Interval> getAllRequestIntervals(SWEKSupplier eventType) {
        return downloadedCache.get(eventType).getAllRequestIntervals();
    }

}
