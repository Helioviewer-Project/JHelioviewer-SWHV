package org.helioviewer.jhv.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.DisplayController;

public class EventCache {

    private static final Set<EventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final Set<EventListener.Highlight> highlightListeners = new HashSet<>();
    private static final ObservationGroups eventGroups = new ObservationGroups();

    private static ObservationGroup lastHighlighted = null;

    public static void registerHandler(EventListener.Handle handler) {
        cacheEventHandlers.add(handler);
    }

    public static void unregisterHandler(EventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    static void fireEventCacheChanged() {
        cacheEventHandlers.forEach(EventListener.Handle::cacheUpdated);
    }

    public static void highlight(ObservationGroup event) {
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

    public static void addHighlightListener(EventListener.Highlight listener) {
        highlightListeners.add(listener);
    }

    public static void removeHighlightListener(EventListener.Highlight listener) {
        highlightListeners.remove(listener);
    }

    private static void fireHighlightChanged() {
        highlightListeners.forEach(EventListener.Highlight::highlightChanged);
        DisplayController.display();
    }

    static void addEvent(SolarEvent event) {
        eventGroups.addEvent(event);
    }

    static void addAssociation(SolarEvent.Link link) {
        eventGroups.addAssociation(link);
    }

    static void replaceEvents(long sequence, List<SolarEvent> events, List<SolarEvent.Link> associations) {
        boolean highlightedGroupCached = eventGroups.contains(lastHighlighted);
        eventGroups.replace(sequence, events, associations);
        if (highlightedGroupCached && !eventGroups.contains(lastHighlighted))
            highlight(null);
        fireEventCacheChanged();
    }

    @Nullable
    public static ObservationGroup getObservationGroup(int id) {
        return eventGroups.getObservationGroup(id);
    }

    public static List<ObservationGroup> getEvents(long start, long end) {
        return eventGroups.getEvents(start, end);
    }

    static void removeSupplier(SWEKSupplier supplier) {
        if (lastHighlighted != null && lastHighlighted.getEvents().stream().anyMatch(event ->
                event.getSupplier() == supplier && eventGroups.getObservationGroup(event.getUniqueID()) == lastHighlighted))
            highlight(null);
        eventGroups.removeSupplier(supplier);
        fireEventCacheChanged();
    }

}
