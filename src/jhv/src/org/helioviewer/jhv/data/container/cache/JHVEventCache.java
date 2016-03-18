package org.helioviewer.jhv.data.container.cache;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

public class JHVEventCache {

    /** singleton instance of JHVevent cache */
    private static JHVEventCache instance;

    /** The events received for a certain date */
    public static class SortedDateInterval implements Comparable<SortedDateInterval> {
        public final long start;
        public final long end;

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

    /** A set with IDs */

    private final Map<String, JHVEvent> allEvents;

    private final Map<String, List<JHVEvent>> missingEventsInEventRelations;

    private final List<JHVEvent> eventsWithRelationRules;

    private final Set<JHVEventType> activeEventTypes;

    private final Map<JHVEventType, RequestCache> downloadedCache;

    private final Map<String, Color> colorPerId;

    /**
     * private default constructor
     */
    private JHVEventCache() {
        events = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>>();
        allEvents = new HashMap<String, JHVEvent>();
        missingEventsInEventRelations = new HashMap<String, List<JHVEvent>>();
        eventsWithRelationRules = new ArrayList<JHVEvent>();
        activeEventTypes = new HashSet<JHVEventType>();
        downloadedCache = new HashMap<JHVEventType, RequestCache>();
        colorPerId = new HashMap<String, Color>();
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
        activeEventTypes.add(event.getJHVEventType());
        JHVEvent savedEvent = allEvents.get(event.getUniqueID());
        if (savedEvent == null) {
            allEvents.put(event.getUniqueID(), event);
            addToList(event);
        }
    }

    private void addToList(JHVEvent event) {
        SortedDateInterval i = new SortedDateInterval(event.getStartDate().getTime(), event.getEndDate().getTime());
        if (!events.containsKey(event.getJHVEventType())) {
            events.put(event.getJHVEventType(), new TreeMap<SortedDateInterval, JHVRelatedEvents>());
        }
        events.get(event.getJHVEventType()).put(i, new JHVRelatedEvents(event));
    }

    /**
     * Gets the events that are available within the interval.
     *
     * @param startDate
     *            start date of the interval
     * @param endDate
     *            end date of the interval
     * @return the list of events that are available in the interval
     */
    public JHVEventCacheResult get(Date startDate, Date endDate, Date extendedStart, Date extendedEnd) {

        //Map<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>> eventsResult = new HashMap<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>>();
        Map<JHVEventType, List<Interval<Date>>> missingIntervals = new HashMap<JHVEventType, List<Interval<Date>>>();
        for (JHVEventType evt : activeEventTypes) {
            /*
            if (events.containsKey(evt)) {
                long delta = 100 * 60 * 60 * 24;
                SortedMap<SortedDateInterval, JHVRelatedEvents> submap = events.get(evt).subMap(new SortedDateInterval(startDate.getTime() - delta, startDate.getTime() - delta), new SortedDateInterval(endDate.getTime() + delta, endDate.getTime() + delta));
                eventsResult.put(evt, submap);
            }*/
            List<Interval<Date>> missing = downloadedCache.get(evt).getMissingIntervals(new Interval<Date>(startDate, endDate));
            if (!missing.isEmpty()) {
                missing = downloadedCache.get(evt).adaptRequestCache(extendedStart, extendedEnd);
                missingIntervals.put(evt, missing);
            }
        }
        return new JHVEventCacheResult(events, missingIntervals);
    }

    /**
     * Removes all the events of the given event type from the event cache.
     *
     * @param eventType
     *            the event type to remove
     */
    public void removeEventType(JHVEventType eventType, boolean keepActive) {
        if (!keepActive) {
            activeEventTypes.remove(eventType);
        } else {
            deleteFromCache(eventType);
        }
    }

    private void deleteFromCache(JHVEventType eventType) {
        downloadedCache.put(eventType, new RequestCache());
        events.remove(eventType);
    }

    private void checkAndFixRelationShip(JHVEvent event) {
        //executeRelationshipRules(event);
    }

    /*
        private void executeRelationshipRules(JHVEvent event) {
            List<JHVEventRelationShipRule> rules = event.getEventRelationShip().getRelationshipRules();
            for (JHVEventRelationShipRule rule : rules) {
                for (JHVEvent candidate : eventsWithRelationRules) {
                    if (candidate.getJHVEventType().getEventType().getEventName().toLowerCase().equals(rule.getRelatedWith().getEventType().getEventName().toLowerCase())) {
                        int foundCorrespondinParameters = 0;

                        for (JHVRelatedOn relatedOn : rule.getRelatedOn()) {

                            String rel = relatedOn.getRelatedOnWith().getParameterName().toLowerCase();

                            Map<String, JHVEventParameter> params = candidate.getAllEventParameters();

                            JHVEventParameter p = params.get(rel);
                            if (p != null && p.getParameterValue() != null) {
                                JHVEventParameter eventP = event.getAllEventParameters().get(rel);
                                if (eventP != null && eventP.getParameterValue() != null) {
                                    if (eventP.getParameterName().toLowerCase().equals(rel) && eventP.getParameterValue().equals(p.getParameterValue())) {
                                        foundCorrespondinParameters++;
                                    }
                                }

                            }
                        }

                        if (foundCorrespondinParameters == rule.getRelatedOn().size()) {
                            event.getEventRelationShip().getRelatedEventsByRule().put(candidate.getUniqueID(), new JHVEventRelation(candidate.getUniqueID(), candidate));
                            candidate.getEventRelationShip().getRelatedEventsByRule().put(event.getUniqueID(), new JHVEventRelation(event.getUniqueID(), event));
                        }
                    }
                }
            }
            eventsWithRelationRules.add(event);
        }
    */
    public Collection<Interval<Date>> getAllRequestIntervals(JHVEventType eventType) {
        return downloadedCache.get(eventType).getAllRequestIntervals();
    }

    public void removeRequestedIntervals(JHVEventType eventType, Interval<Date> interval) {
        downloadedCache.get(eventType).removeRequestedIntervals(interval);
    }

    public void eventTypeActivated(JHVEventType eventType) {
        activeEventTypes.add(eventType);
        if (!downloadedCache.containsKey(eventType)) {
            downloadedCache.put(eventType, new RequestCache());
        }
    }

}
