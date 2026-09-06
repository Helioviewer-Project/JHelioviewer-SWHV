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

final class ObservationGroups {

    private final NavigableMap<Long, List<ObservationGroup>> events = new TreeMap<>();
    private final Map<Integer, ObservationGroup> relatedEventsById = new HashMap<>();
    private final Map<Integer, Set<SolarEvent.Link>> pendingAssocs = new HashMap<>();
    private final Map<Integer, Long> eventSequences = new HashMap<>();
    private final Map<SolarEvent.Link, Long> associationSequences = new HashMap<>();
    private long maximumGroupDuration;

    // A decoded observation supplies its complete incident-link set for this snapshot.
    void replace(long sequence, List<SolarEvent> observations, List<SolarEvent.Link> associations) {
        Set<Integer> refreshedIds = new HashSet<>();
        for (SolarEvent event : observations) {
            if (eventSequences.getOrDefault(event.getUniqueID(), 0L) <= sequence)
                refreshedIds.add(event.getUniqueID());
        }
        Set<SolarEvent.Link> currentLinks = new HashSet<>(associations);
        Set<SolarEvent.Link> removedLinks = new HashSet<>();
        for (Map.Entry<SolarEvent.Link, Long> entry : associationSequences.entrySet()) {
            SolarEvent.Link link = entry.getKey();
            if (entry.getValue() <= sequence && (refreshedIds.contains(link.firstId()) || refreshedIds.contains(link.secondId()))
                    && !currentLinks.contains(link))
                removedLinks.add(link);
        }
        Set<ObservationGroup> affectedGroups = new HashSet<>();
        for (SolarEvent.Link link : removedLinks) {
            associationSequences.remove(link);
            ObservationGroup group = relatedEventsById.get(link.firstId());
            if (group != null && group.getAssociations().contains(link))
                affectedGroups.add(group);
        }
        pendingAssocs.values().forEach(links -> links.removeAll(removedLinks));
        pendingAssocs.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        for (ObservationGroup group : affectedGroups)
            rebuild(group, Set.of(), removedLinks);

        for (SolarEvent event : observations) {
            if (refreshedIds.contains(event.getUniqueID())) {
                eventSequences.put(event.getUniqueID(), sequence);
                addEvent(event);
            }
        }
        for (SolarEvent.Link link : associations) {
            if (eventSequences.getOrDefault(link.firstId(), 0L) <= sequence && eventSequences.getOrDefault(link.secondId(), 0L) <= sequence
                    && associationSequences.getOrDefault(link, 0L) <= sequence) {
                associationSequences.put(link, sequence);
                addAssociation(link);
            }
        }
    }

    boolean contains(ObservationGroup group) {
        return group != null && group.getEvents().stream().anyMatch(event -> relatedEventsById.get(event.getUniqueID()) == group);
    }

    void addEvent(SolarEvent event) {
        Integer id = event.getUniqueID();
        ObservationGroup relatedEvents = relatedEventsById.get(id);
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
    ObservationGroup getObservationGroup(int id) {
        return relatedEventsById.get(id);
    }

    private void resolvePendingAssociations(Integer id) {
        Set<SolarEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(this::addAssociation);
    }

    private void addNewRelatedEvent(SolarEvent event) {
        addNewRelatedEvent(event, new ObservationGroup(event));
    }

    private void addNewRelatedEvent(SolarEvent event, ObservationGroup relatedEvents) {
        addToIndex(relatedEvents);
        relatedEventsById.put(event.getUniqueID(), relatedEvents);
    }

    private void merge(ObservationGroup current, ObservationGroup found) {
        if (current == found) return;
        removeFromIndex(current);
        removeFromIndex(found);
        current.merge(found);
        addToIndex(current);
        for (SolarEvent foundev : found.getEvents()) {
            relatedEventsById.put(foundev.getUniqueID(), current);
        }
    }

    private void addToIndex(ObservationGroup event) {
        events.computeIfAbsent(event.getStart(), _ -> new ArrayList<>()).add(event);
        maximumGroupDuration = Math.max(maximumGroupDuration, event.getEnd() - event.getStart());
    }

    private void removeFromIndex(ObservationGroup event) {
        List<ObservationGroup> list = events.get(event.getStart());
        if (list == null)
            return;

        list.remove(event);
        if (list.isEmpty())
            events.remove(event.getStart());
    }

    void addAssociation(SolarEvent.Link link) {
        associationSequences.putIfAbsent(link, 0L);
        ObservationGroup first = relatedEventsById.get(link.firstId());
        ObservationGroup second = relatedEventsById.get(link.secondId());
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

    private void addPendingAssociation(int id, SolarEvent.Link link) {
        pendingAssocs.computeIfAbsent(id, k -> new HashSet<>()).add(link);
    }

    List<ObservationGroup> getEvents(long start, long end) {
        if (events.isEmpty()) return Collections.emptyList();
        List<ObservationGroup> result = new ArrayList<>();
        NavigableMap<Long, List<ObservationGroup>> relevantRange =
                events.subMap(start - maximumGroupDuration, true, end, true);
        for (List<ObservationGroup> list : relevantRange.values()) {
            for (ObservationGroup event : list) {
                if (event.getEnd() >= start && event.overlaps(start, end))
                    result.add(event);
            }
        }
        return result;
    }

    void removeSupplier(SWEKSupplier supplier) {
        Set<ObservationGroup> affectedGroups = new HashSet<>();
        Set<Integer> removedIds = new HashSet<>();
        for (List<ObservationGroup> groups : events.values()) {
            for (ObservationGroup group : groups) {
                for (SolarEvent event : group.getEvents()) {
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
        removedIds.forEach(eventSequences::remove);
        associationSequences.keySet().removeIf(link -> removedIds.contains(link.firstId()) || removedIds.contains(link.secondId()));

        for (ObservationGroup group : affectedGroups)
            rebuild(group, removedIds, Set.of());

        maximumGroupDuration = 0;
        for (List<ObservationGroup> groups : events.values()) {
            for (ObservationGroup group : groups)
                maximumGroupDuration = Math.max(maximumGroupDuration, group.getEnd() - group.getStart());
        }
    }

    private void rebuild(ObservationGroup group, Set<Integer> removedIds, Set<SolarEvent.Link> removedLinks) {
        removeFromIndex(group);
        for (SolarEvent event : group.getEvents())
            relatedEventsById.remove(event.getUniqueID());

        for (SolarEvent event : group.getEvents()) {
            if (!removedIds.contains(event.getUniqueID()))
                addNewRelatedEvent(event, new ObservationGroup(event, group.getColor()));
        }
        for (SolarEvent.Link link : group.getAssociations()) {
            if (!removedLinks.contains(link) && relatedEventsById.containsKey(link.firstId()) && relatedEventsById.containsKey(link.secondId()))
                addAssociation(link);
        }
    }

}
