package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

final class ObservationGroups {

    private static final class ObservationEntry {
        private ObservationGroup group;
        private long sequence;

        ObservationEntry(ObservationGroup _group) {
            group = _group;
        }
    }

    private final NavigableMap<Long, List<ObservationGroup>> events = new TreeMap<>();
    private final Map<Integer, ObservationEntry> observationsById = new HashMap<>();
    private final Map<Integer, Set<SolarEvent.Link>> pendingAssocs = new HashMap<>();
    private final Map<SolarEvent.Link, Long> associationSequences = new HashMap<>();
    private long maximumGroupDuration;

    // A decoded observation supplies its complete incident-link set for this snapshot.
    void replace(EventBatch batch) {
        long sequence = batch.sequence();
        List<SolarEvent> observations = batch.events();
        List<SolarEvent.Link> associations = batch.associations();
        Set<Integer> refreshedIds = new HashSet<>();
        for (SolarEvent event : observations) {
            if (getSequence(event.getUniqueID()) <= sequence)
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
            ObservationGroup group = getObservationGroup(link.firstId());
            if (group != null && group.getAssociations().contains(link))
                affectedGroups.add(group);
        }
        pendingAssocs.values().forEach(links -> links.removeAll(removedLinks));
        pendingAssocs.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        for (ObservationGroup group : affectedGroups)
            rebuild(group, Set.of(), removedLinks);

        for (SolarEvent event : observations) {
            if (refreshedIds.contains(event.getUniqueID())) {
                addEvent(event);
                observationsById.get(event.getUniqueID()).sequence = sequence;
            }
        }
        for (SolarEvent.Link link : associations) {
            if (getSequence(link.firstId()) <= sequence && getSequence(link.secondId()) <= sequence
                    && associationSequences.getOrDefault(link, 0L) <= sequence) {
                associationSequences.put(link, sequence);
                addAssociation(link);
            }
        }
    }

    boolean contains(ObservationGroup group) {
        return group != null && group.getEvents().stream().anyMatch(event -> getObservationGroup(event.getUniqueID()) == group);
    }

    void addEvent(SolarEvent event) {
        Integer id = event.getUniqueID();
        ObservationGroup relatedEvents = getObservationGroup(id);
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
        ObservationEntry entry = observationsById.get(id);
        return entry == null ? null : entry.group;
    }

    private long getSequence(int id) {
        ObservationEntry entry = observationsById.get(id);
        return entry == null ? 0 : entry.sequence;
    }

    private void resolvePendingAssociations(Integer id) {
        Set<SolarEvent.Link> pending = pendingAssocs.remove(id);
        if (pending != null)
            pending.forEach(this::addAssociation);
    }

    private void addNewRelatedEvent(SolarEvent event) {
        ObservationGroup group = new ObservationGroup(event);
        addToIndex(group);
        observationsById.put(event.getUniqueID(), new ObservationEntry(group));
    }

    private void merge(ObservationGroup current, ObservationGroup found) {
        if (current == found) return;
        current.merge(found);
        for (SolarEvent foundev : found.getEvents()) {
            observationsById.get(foundev.getUniqueID()).group = current;
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
        ObservationGroup first = getObservationGroup(link.firstId());
        ObservationGroup second = getObservationGroup(link.secondId());
        if (first != null && second != null) {
            if (first != second) {
                removeFromIndex(first);
                removeFromIndex(second);
                merge(first, second);
                addToIndex(first);
            }
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
        // Preserve reinsertion order for groups sharing the same start time.
        LinkedHashSet<ObservationGroup> rebuilt = new LinkedHashSet<>();
        for (SolarEvent event : group.getEvents()) {
            if (removedIds.contains(event.getUniqueID())) {
                observationsById.remove(event.getUniqueID());
            } else {
                ObservationGroup replacement = new ObservationGroup(event, group.getColor());
                observationsById.get(event.getUniqueID()).group = replacement;
                rebuilt.add(replacement);
            }
        }
        for (SolarEvent.Link link : group.getAssociations()) {
            if (removedLinks.contains(link))
                continue;
            ObservationGroup first = getObservationGroup(link.firstId()), second = getObservationGroup(link.secondId());
            if (first == null || second == null)
                continue;
            if (first != second) {
                merge(first, second);
                rebuilt.remove(second);
                rebuilt.addLast(first);
            }
            first.addAssociation(link);
        }
        rebuilt.forEach(this::addToIndex);
    }

}
