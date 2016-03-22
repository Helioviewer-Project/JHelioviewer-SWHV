package org.helioviewer.jhv.data.container.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

public class JHVEventCache {

    /** singleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    public static class SortedDateInterval implements Comparable<SortedDateInterval> {
        public long start;
        public long end;

        public SortedDateInterval(long _start, long _end) {
            start = _start;
            end = _end;
        }

        @Override
        public int compareTo(SortedDateInterval o2) {
            if (this.start < o2.start)
                return -1;
            else if (this.start == o2.start && this.end < o2.end) {
                return -1;
            }
            else if (this.start == o2.start && this.end == o2.end)
                return 0;
            return 1;
        }
    }

    private final Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events;

    private final Map<Integer, JHVRelatedEvents> relEvents = new HashMap<Integer, JHVRelatedEvents>();;

    private final Set<JHVEventType> activeEventTypes;

    private final Map<JHVEventType, RequestCache> downloadedCache;

    private final Map<Integer, ArrayList<JHVAssociation>> assoLeft = new HashMap<Integer, ArrayList<JHVAssociation>>();
    private final Map<Integer, ArrayList<JHVAssociation>> assoRight = new HashMap<Integer, ArrayList<JHVAssociation>>();

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        activeEventTypes = new HashSet<JHVEventType>();
        downloadedCache = new HashMap<JHVEventType, RequestCache>();
    }

    /**
     * Gets the singleton instance of the JHV event cache.
     *
     * @return singleton instance of the cache.
     */
    public static JHVEventCache getSingletonInstance() {
        if (instance == null) {
            instance = new JHVEventCache();
        }
        return instance;
    }

    public void add(JHVEvent event) {
        if (relEvents.containsKey(event.getUniqueID())) {
            return;
        }

        JHVEventType evtType = event.getJHVEventType();

        if (!events.containsKey(evtType)) {
            events.put(evtType, new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        JHVRelatedEvents current = null;
        current = checkAssociation(current, assoLeft, true, event);
        current = checkAssociation(current, assoRight, true, event);
        if (current == null) {
            current = new JHVRelatedEvents(event, events);
            relEvents.put(event.getUniqueID(), current);
        }
    }

    private JHVRelatedEvents checkAssociation(JHVRelatedEvents current, Map<Integer, ArrayList<JHVAssociation>> assoList, boolean isLeft, JHVEvent event) {
        Integer uid = event.getUniqueID();
        if (assoList.containsKey(uid)) {
            for (Iterator<JHVAssociation> iterator = assoList.get(uid).iterator(); iterator.hasNext();) {
                JHVAssociation tocheck = iterator.next();
                Integer founduid = isLeft ? tocheck.left : tocheck.right;
                JHVRelatedEvents found = relEvents.get(founduid);
                if (found != null) {
                    if (current == null) {
                        found.add(event, events);
                        found.addAssociation(tocheck);
                        relEvents.put(uid, found);
                        current = found;
                    }
                    else {
                        if (current != found) {
                            merge(current, found);
                            current.addAssociation(tocheck);
                        }
                    }
                    iterator.remove();
                }
            }
            if (assoList.get(uid).isEmpty()) {
                assoList.remove(uid);
            }
        }
        return current;
    }

    private void merge(JHVRelatedEvents current, JHVRelatedEvents found) {
        current.merge(found, events);
        for (JHVEvent foundev : found.getEvents()) {
            Integer key = foundev.getUniqueID();
            relEvents.remove(key);
            relEvents.put(key, current);
        }
    }

    private void addAssociation(boolean isLeft, JHVAssociation association) {
        Integer key = isLeft ? association.left : association.right;
        Map<Integer, ArrayList<JHVAssociation>> assoMap = isLeft ? assoLeft : assoRight;
        ArrayList<JHVAssociation> assocs = assoMap.get(key);
        if (assocs == null) {
            assocs = new ArrayList<JHVAssociation>();
            assoMap.put(key, assocs);
        }
        assocs.add(association);
    }

    public void add(JHVAssociation association) {
        if (relEvents.containsKey(association.left) && relEvents.containsKey(association.right)) {
            JHVRelatedEvents ll = relEvents.get(association.left);
            JHVRelatedEvents rr = relEvents.get(association.right);
            if (ll != rr) {
                merge(ll, rr);
                ll.addAssociation(association);
            }
        }
        else {
            addAssociation(true, association);
            addAssociation(false, association);
        }
    }

    public JHVEventCacheResult get(Date startDate, Date endDate, Date extendedStart, Date extendedEnd) {

        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsResult = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        Map<JHVEventType, List<Interval<Date>>> missingIntervals = new HashMap<JHVEventType, List<Interval<Date>>>();
        for (JHVEventType evt : activeEventTypes) {
            SortedMap<SortedDateInterval, JHVRelatedEvents> sortedEvents = events.get(evt);
            if (sortedEvents != null) {
                long delta = 1000 * 60 * 60 * 24;
                SortedMap<SortedDateInterval, JHVRelatedEvents> submap = sortedEvents.subMap(new SortedDateInterval(startDate.getTime() - delta, startDate.getTime() - delta), new SortedDateInterval(endDate.getTime() + delta, endDate.getTime() + delta));
                eventsResult.put(evt, submap);
            }
            List<Interval<Date>> missing = downloadedCache.get(evt).getMissingIntervals(new Interval<Date>(startDate, endDate));
            if (!missing.isEmpty()) {
                missing = downloadedCache.get(evt).adaptRequestCache(extendedStart, extendedEnd);
                missingIntervals.put(evt, missing);
            }
        }
        return new JHVEventCacheResult(eventsResult, missingIntervals);
    }

    public void removeEventType(JHVEventType eventType, boolean keepActive) {
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
    }

    public Collection<Interval<Date>> getAllRequestIntervals(JHVEventType eventType) {
        return downloadedCache.get(eventType).getAllRequestIntervals();
    }

    public void removeRequestedIntervals(JHVEventType eventType, Interval<Date> interval) {
        downloadedCache.get(eventType).removeRequestedIntervals(interval);
    }

    public void eventTypeActivated(JHVEventType eventType) {
        activeEventTypes.add(eventType);
        if (!downloadedCache.containsKey(eventType)) {
            RequestCache cache = new RequestCache();
            downloadedCache.put(eventType, cache);
        }
    }

}
