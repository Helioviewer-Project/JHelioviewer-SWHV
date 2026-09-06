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

final class JHVObservationGroups {

    private final NavigableMap<Long, List<JHVObservationGroup>> events = new TreeMap<>();
    private final Map<Integer, JHVObservationGroup> relatedEventsById = new HashMap<>();
    private final Map<Integer, Set<JHVEvent.Link>> pendingAssocs = new HashMap<>();
    private long maximumGroupDuration;

    void addEvent(JHVEvent event) {
        Integer id = event.getUniqueID();
        JHVObservationGroup relatedEvents = relatedEventsById.get(id);
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
    JHVObservationGroup getObservationGroup(int id) {
        return relatedEventsById.get(id);
    }

    private void resolvePendingAssociations(Integer id) {
        Set<JHVEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(this::addAssociation);
    }

    private void addNewRelatedEvent(JHVEvent event) {
        addNewRelatedEvent(event, new JHVObservationGroup(event));
    }

    private void addNewRelatedEvent(JHVEvent event, JHVObservationGroup relatedEvents) {
        addToIndex(relatedEvents);
        relatedEventsById.put(event.getUniqueID(), relatedEvents);
    }

    private void merge(JHVObservationGroup current, JHVObservationGroup found) {
        if (current == found) return;
        removeFromIndex(current);
        removeFromIndex(found);
        current.merge(found);
        addToIndex(current);
        for (JHVEvent foundev : found.getEvents()) {
            relatedEventsById.put(foundev.getUniqueID(), current);
        }
    }

    private void addToIndex(JHVObservationGroup event) {
        events.computeIfAbsent(event.getStart(), _ -> new ArrayList<>()).add(event);
        maximumGroupDuration = Math.max(maximumGroupDuration, event.getEnd() - event.getStart());
    }

    private void removeFromIndex(JHVObservationGroup event) {
        List<JHVObservationGroup> list = events.get(event.getStart());
        if (list == null)
            return;

        list.remove(event);
        if (list.isEmpty())
            events.remove(event.getStart());
    }

    void addAssociation(JHVEvent.Link link) {
        JHVObservationGroup first = relatedEventsById.get(link.firstId());
        JHVObservationGroup second = relatedEventsById.get(link.secondId());
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

    private void addPendingAssociation(int id, JHVEvent.Link link) {
        pendingAssocs.computeIfAbsent(id, k -> new HashSet<>()).add(link);
    }

    List<JHVObservationGroup> getEvents(long start, long end) {
        if (events.isEmpty()) return Collections.emptyList();
        List<JHVObservationGroup> result = new ArrayList<>();
        NavigableMap<Long, List<JHVObservationGroup>> relevantRange =
                events.subMap(start - maximumGroupDuration, true, end, true);
        for (List<JHVObservationGroup> list : relevantRange.values()) {
            for (JHVObservationGroup event : list) {
                if (event.getEnd() >= start && event.overlaps(start, end))
                    result.add(event);
            }
        }
        return result;
    }

    void removeSupplier(SWEKSupplier supplier) {
        Set<JHVObservationGroup> affectedGroups = new HashSet<>();
        Set<Integer> removedIds = new HashSet<>();
        for (List<JHVObservationGroup> groups : events.values()) {
            for (JHVObservationGroup group : groups) {
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

        for (JHVObservationGroup group : affectedGroups)
            rebuildWithout(group, supplier);

        maximumGroupDuration = 0;
        for (List<JHVObservationGroup> groups : events.values()) {
            for (JHVObservationGroup group : groups)
                maximumGroupDuration = Math.max(maximumGroupDuration, group.getEnd() - group.getStart());
        }
    }

    private void rebuildWithout(JHVObservationGroup group, SWEKSupplier supplier) {
        removeFromIndex(group);
        for (JHVEvent event : group.getEvents())
            relatedEventsById.remove(event.getUniqueID());

        for (JHVEvent event : group.getEvents()) {
            if (event.getSupplier() != supplier)
                addNewRelatedEvent(event, new JHVObservationGroup(event, group.getColor()));
        }
        for (JHVEvent.Link link : group.getAssociations()) {
            if (relatedEventsById.containsKey(link.firstId()) && relatedEventsById.containsKey(link.secondId()))
                addAssociation(link);
        }
    }

}
