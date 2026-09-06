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

final class EventCollection {

    private static final class EventEntry {
        private RelatedEvents relatedEvents;
        private long snapshotSequence;

        EventEntry(RelatedEvents _relatedEvents) {
            relatedEvents = _relatedEvents;
        }
    }

    private final NavigableMap<Long, List<RelatedEvents>> relatedEventsByStart = new TreeMap<>();
    private final Map<Integer, EventEntry> eventsById = new HashMap<>();
    private final Map<Integer, Set<SolarEvent.Link>> pendingLinks = new HashMap<>();
    private final Map<SolarEvent.Link, Long> linkSequences = new HashMap<>();
    private long maximumDuration;

    // A decoded event supplies its complete incident-link set for this snapshot.
    void replace(EventBatch batch) {
        long snapshotSequence = batch.sequence();
        List<SolarEvent> updatedEvents = batch.events();
        List<SolarEvent.Link> associations = batch.associations();
        Set<Integer> refreshedIds = new HashSet<>();
        for (SolarEvent event : updatedEvents) {
            if (getEventSequence(event.getUniqueID()) <= snapshotSequence)
                refreshedIds.add(event.getUniqueID());
        }
        Set<SolarEvent.Link> currentLinks = new HashSet<>(associations);
        Set<SolarEvent.Link> removedLinks = new HashSet<>();
        for (Map.Entry<SolarEvent.Link, Long> entry : linkSequences.entrySet()) {
            SolarEvent.Link link = entry.getKey();
            if (entry.getValue() <= snapshotSequence && (refreshedIds.contains(link.firstId()) || refreshedIds.contains(link.secondId()))
                    && !currentLinks.contains(link))
                removedLinks.add(link);
        }
        Set<RelatedEvents> affectedSets = new HashSet<>();
        for (SolarEvent.Link link : removedLinks) {
            linkSequences.remove(link);
            removePendingAssociation(link.firstId(), link);
            removePendingAssociation(link.secondId(), link);
            RelatedEvents relatedEvents = getRelatedEvents(link.firstId());
            if (relatedEvents != null && relatedEvents.getAssociations().contains(link))
                affectedSets.add(relatedEvents);
        }
        for (RelatedEvents relatedEvents : affectedSets)
            rebuild(relatedEvents, Set.of(), removedLinks);

        for (SolarEvent event : updatedEvents) {
            if (refreshedIds.contains(event.getUniqueID())) {
                addEvent(event);
                eventsById.get(event.getUniqueID()).snapshotSequence = snapshotSequence;
            }
        }
        for (SolarEvent.Link link : associations) {
            if (getEventSequence(link.firstId()) <= snapshotSequence && getEventSequence(link.secondId()) <= snapshotSequence
                    && linkSequences.getOrDefault(link, 0L) <= snapshotSequence) {
                linkSequences.put(link, snapshotSequence);
                addAssociation(link);
            }
        }
    }

    boolean contains(RelatedEvents relatedEvents) {
        return relatedEvents != null && relatedEvents.getEvents().stream().anyMatch(event -> getRelatedEvents(event.getUniqueID()) == relatedEvents);
    }

    void addEvent(SolarEvent event) {
        int id = event.getUniqueID();
        RelatedEvents relatedEvents = getRelatedEvents(id);
        if (relatedEvents != null) {
            removeFromIndex(relatedEvents);
            relatedEvents.replaceEvent(event);
            addToIndex(relatedEvents);
        } else {
            addStandaloneEvent(event);
        }
        resolvePendingAssociations(id);
    }

    @Nullable
    RelatedEvents getRelatedEvents(int id) {
        EventEntry entry = eventsById.get(id);
        return entry == null ? null : entry.relatedEvents;
    }

    private long getEventSequence(int id) {
        EventEntry entry = eventsById.get(id);
        return entry == null ? 0 : entry.snapshotSequence;
    }

    private void resolvePendingAssociations(int id) {
        Set<SolarEvent.Link> pending = pendingLinks.remove(id);
        if (pending != null)
            pending.forEach(this::addAssociation);
    }

    private void addStandaloneEvent(SolarEvent event) {
        RelatedEvents relatedEvents = new RelatedEvents(event);
        addToIndex(relatedEvents);
        eventsById.put(event.getUniqueID(), new EventEntry(relatedEvents));
    }

    private void merge(RelatedEvents current, RelatedEvents found) {
        if (current == found) return;
        current.merge(found);
        for (SolarEvent event : found.getEvents()) {
            eventsById.get(event.getUniqueID()).relatedEvents = current;
        }
    }

    private void addToIndex(RelatedEvents relatedEvents) {
        relatedEventsByStart.computeIfAbsent(relatedEvents.getStart(), _ -> new ArrayList<>()).add(relatedEvents);
        maximumDuration = Math.max(maximumDuration, relatedEvents.getEnd() - relatedEvents.getStart());
    }

    private void removeFromIndex(RelatedEvents relatedEvents) {
        List<RelatedEvents> list = relatedEventsByStart.get(relatedEvents.getStart());
        if (list == null)
            return;

        list.remove(relatedEvents);
        if (list.isEmpty())
            relatedEventsByStart.remove(relatedEvents.getStart());
    }

    void addAssociation(SolarEvent.Link link) {
        linkSequences.putIfAbsent(link, 0L);
        RelatedEvents first = getRelatedEvents(link.firstId());
        RelatedEvents second = getRelatedEvents(link.secondId());
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
        pendingLinks.computeIfAbsent(id, k -> new HashSet<>()).add(link);
    }

    private void removePendingAssociation(int id, SolarEvent.Link link) {
        Set<SolarEvent.Link> pending = pendingLinks.get(id);
        if (pending == null)
            return;
        pending.remove(link);
        if (pending.isEmpty())
            pendingLinks.remove(id);
    }

    List<RelatedEvents> getEvents(long start, long end) {
        if (relatedEventsByStart.isEmpty()) return Collections.emptyList();
        List<RelatedEvents> result = new ArrayList<>();
        NavigableMap<Long, List<RelatedEvents>> relevantRange =
                relatedEventsByStart.subMap(start - maximumDuration, true, end, true);
        for (List<RelatedEvents> list : relevantRange.values()) {
            for (RelatedEvents relatedEvents : list) {
                if (relatedEvents.getEnd() >= start && relatedEvents.overlaps(start, end))
                    result.add(relatedEvents);
            }
        }
        return result;
    }

    void removeSupplier(SWEKSupplier supplier) {
        Set<RelatedEvents> affectedSets = new HashSet<>();
        Set<Integer> removedIds = new HashSet<>();
        for (List<RelatedEvents> relatedSets : relatedEventsByStart.values()) {
            for (RelatedEvents relatedEvents : relatedSets) {
                for (SolarEvent event : relatedEvents.getEvents()) {
                    if (event.getSupplier() == supplier) {
                        affectedSets.add(relatedEvents);
                        removedIds.add(event.getUniqueID());
                    }
                }
            }
        }

        if (affectedSets.isEmpty())
            return;

        pendingLinks.values().forEach(links -> links.removeIf(link ->
                removedIds.contains(link.firstId()) || removedIds.contains(link.secondId())));
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        linkSequences.keySet().removeIf(link -> removedIds.contains(link.firstId()) || removedIds.contains(link.secondId()));

        for (RelatedEvents relatedEvents : affectedSets)
            rebuild(relatedEvents, removedIds, Set.of());

        maximumDuration = 0;
        for (List<RelatedEvents> relatedSets : relatedEventsByStart.values()) {
            for (RelatedEvents relatedEvents : relatedSets)
                maximumDuration = Math.max(maximumDuration, relatedEvents.getEnd() - relatedEvents.getStart());
        }
    }

    private void rebuild(RelatedEvents relatedEvents, Set<Integer> removedIds, Set<SolarEvent.Link> removedLinks) {
        removeFromIndex(relatedEvents);
        // Preserve reinsertion order for related events sharing the same start time.
        LinkedHashSet<RelatedEvents> rebuilt = new LinkedHashSet<>();
        for (SolarEvent event : relatedEvents.getEvents()) {
            if (removedIds.contains(event.getUniqueID())) {
                eventsById.remove(event.getUniqueID());
            } else {
                RelatedEvents replacement = new RelatedEvents(event, relatedEvents.getColor());
                eventsById.get(event.getUniqueID()).relatedEvents = replacement;
                rebuilt.add(replacement);
            }
        }
        for (SolarEvent.Link link : relatedEvents.getAssociations()) {
            if (removedLinks.contains(link))
                continue;
            RelatedEvents first = getRelatedEvents(link.firstId()), second = getRelatedEvents(link.secondId());
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
