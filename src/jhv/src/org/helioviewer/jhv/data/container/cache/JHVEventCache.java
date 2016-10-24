package org.helioviewer.jhv.data.container.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.container.JHVEventCacheRequestHandler;
import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;

public class JHVEventCache {
    private static final HashSet<JHVEventHandler> cacheEventHandlers = new HashSet<JHVEventHandler>();

    private JHVEventCacheRequestHandler incomingRequestManager;

    private static final double factor = 0.2;

    private static JHVRelatedEvents lastHighlighted = null;

    private static JHVEventCache instance;

    /** The events received for a certain date */
    public static class SortedDateInterval implements Comparable<SortedDateInterval> {

        public long start;
        public long end;
        private final int id;
        private static int id_gen = Integer.MIN_VALUE;

        public SortedDateInterval(long _start, long _end) {
            start = _start;
            end = _end;
            id = id_gen++;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SortedDateInterval && compareTo((SortedDateInterval) o) == 0;
        }

        @Override
        public int hashCode() {
            assert false : "hashCode not designed";
        return 42;
        }

        @Override
        public int compareTo(SortedDateInterval o2) {
            if (start < o2.start) {
                return -1;
            } else if (start == o2.start && end < o2.end) {
                return -1;
            } else if (start == o2.start && end == o2.end && o2.id < id) {
                return -1;
            } else if (start == o2.start && end == o2.end && o2.id == id) {
                return 0;
            }
            return 1;
        }

    }

    private final Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events;

    private final Map<Integer, JHVRelatedEvents> relEvents = new HashMap<Integer, JHVRelatedEvents>();

    private final Set<JHVEventType> activeEventTypes;

    private final Map<JHVEventType, RequestCache> downloadedCache;

    private final ArrayList<JHVAssociation> assocs = new ArrayList<JHVAssociation>();

    private JHVEventCache() {
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        activeEventTypes = new HashSet<JHVEventType>();
        downloadedCache = new HashMap<JHVEventType, RequestCache>();
    }

    public static JHVEventCache getSingletonInstance() {
        if (instance == null) {
            instance = new JHVEventCache();
        }
        return instance;
    }

    public void requestForInterval(final long startDate, final long endDate, final JHVEventHandler handler) {
        long deltaT = Math.max((long) ((endDate - startDate) * factor), TimeUtils.DAY_IN_MILLIS);
        long newStartDate = startDate - deltaT;
        long newEndDate = endDate + deltaT;
        cacheEventHandlers.add(handler);

        JHVEventCacheResult result = get(startDate, endDate, newStartDate, newEndDate);
        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = result.getAvailableEvents();
        handler.newEventsReceived(events);
        for (JHVEventType eventType : result.getMissingIntervals().keySet()) {
            List<Interval> missingList = result.getMissingIntervals().get(eventType);
            for (Interval missing : missingList) {
                requestEvents(eventType, missing);
            }
        }
    }

    public void finishedDownload(boolean partially) {
        fireEventCacheChanged();
    }

    public void removeEvents(final JHVEventType eventType, boolean keepActive) {
        removeEventType(eventType, keepActive);
        fireEventCacheChanged();
    }

    private void requestEvents(JHVEventType eventType, Interval interval) {
        incomingRequestManager.handleRequestForInterval(eventType, interval);
    }

    private void fireEventCacheChanged() {
        for (JHVEventHandler handler : cacheEventHandlers) {
            handler.cacheUpdated();
        }
    }

    public void intervalsNotDownloaded(JHVEventType eventType, Interval interval) {
        removeRequestedIntervals(eventType, interval);
        get(interval.start, interval.end, interval.start, interval.end);
    }

    public void eventTypeActivated(JHVEventType eventType) {
        _eventTypeActivated(eventType);
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

    public void registerHandler(JHVEventCacheRequestHandler incomingRequestManager) {
        this.incomingRequestManager = incomingRequestManager;
    }

    public void add(JHVEvent event) {
        Integer id = event.getUniqueID();
        if (relEvents.containsKey(id)) {
            relEvents.get(id).swapEvent(event, events);
        } else {
            createNewRelatedEvent(event);
        }
        checkAssociation(event);
    }

    public JHVRelatedEvents getRelatedEvents(int id) {
        return relEvents.get(id);
    }

    private void checkAssociation(JHVEvent event) {
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

    private void createNewRelatedEvent(JHVEvent event) {
        JHVRelatedEvents revent = new JHVRelatedEvents(event, events);
        relEvents.put(event.getUniqueID(), revent);
    }

    private void merge(JHVRelatedEvents current, JHVRelatedEvents found) {
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

    public void add(JHVAssociation association) {
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

    public JHVEventCacheResult get(long startDate, long endDate, long extendedStart, long extendedEnd) {
        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsResult = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        Map<JHVEventType, List<Interval>> missingIntervals = new HashMap<JHVEventType, List<Interval>>();

        for (JHVEventType evt : activeEventTypes) {
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

    private void removeEventType(JHVEventType eventType, boolean keepActive) {
        if (!keepActive) {
            activeEventTypes.remove(eventType);
        } else {
            deleteFromCache(eventType);
        }
    }

    private void deleteFromCache(JHVEventType eventType) {
        RequestCache cache = new RequestCache();
        downloadedCache.put(eventType, cache);
        events.remove(eventType);

        Iterator<Map.Entry<Integer, JHVRelatedEvents>> it = relEvents.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, JHVRelatedEvents> pair = it.next();
            if (pair.getValue().getJHVEventType() == eventType) {
                it.remove();
            }
        }
    }

    public Collection<Interval> getAllRequestIntervals(JHVEventType eventType) {
        return downloadedCache.get(eventType).getAllRequestIntervals();
    }

    private void removeRequestedIntervals(JHVEventType eventType, Interval interval) {
        downloadedCache.get(eventType).removeRequestedInterval(interval);
    }

    private void _eventTypeActivated(JHVEventType eventType) {
        activeEventTypes.add(eventType);
        if (!downloadedCache.containsKey(eventType)) {
            RequestCache cache = new RequestCache();
            downloadedCache.put(eventType, cache);
        }
    }

    public void reset(SWEKEventType eventType) {
        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            JHVEventType evt = JHVEventType.getJHVEventType(eventType, supplier);
            downloadedCache.remove(evt);
            downloadedCache.put(evt, new RequestCache());
        }
    }

    public boolean hasData() {
        return !events.isEmpty();
    }

}
