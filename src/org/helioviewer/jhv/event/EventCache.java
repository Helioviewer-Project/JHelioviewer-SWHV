package org.helioviewer.jhv.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.DisplayController;

public class EventCache {

    private static final Set<EventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final Set<EventListener.Highlight> highlightListeners = new HashSet<>();
    private static final EventCollection eventCollection = new EventCollection();

    private static RelatedEvents lastHighlighted = null;

    public static void registerHandler(EventListener.Handle handler) {
        cacheEventHandlers.add(handler);
    }

    public static void unregisterHandler(EventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    static void fireEventCacheChanged() {
        cacheEventHandlers.forEach(EventListener.Handle::cacheUpdated);
    }

    public static void highlight(RelatedEvents event) {
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

    static void replaceEvents(EventBatch batch) {
        boolean highlightCached = eventCollection.contains(lastHighlighted);
        eventCollection.replace(batch);
        if (highlightCached && !eventCollection.contains(lastHighlighted))
            highlight(null);
        fireEventCacheChanged();
    }

    @Nullable
    public static RelatedEvents getRelatedEvents(int id) {
        return eventCollection.getRelatedEvents(id);
    }

    public static List<RelatedEvents> getEvents(long start, long end) {
        return eventCollection.getEvents(start, end);
    }

    static void removeSupplier(SWEKSupplier supplier) {
        if (lastHighlighted != null && lastHighlighted.getEvents().stream().anyMatch(event ->
                event.getSupplier() == supplier && eventCollection.getRelatedEvents(event.getUniqueID()) == lastHighlighted))
            highlight(null);
        eventCollection.removeSupplier(supplier);
        fireEventCacheChanged();
    }

}
