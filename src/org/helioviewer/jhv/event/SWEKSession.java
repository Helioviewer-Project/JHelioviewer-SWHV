package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import javax.annotation.Nullable;

// One plugin activation. Mutable state and notifications belong to the UI thread;
// background details queries use the immutable catalog and serialized database.
public final class SWEKSession implements AutoCloseable {

    public record EventDetails(SolarEvent event, List<SolarEvent> relatedEvents) {}

    private final SWEKCatalog catalog;
    private final EventDatabase database;
    private final SWEKDownloader downloader;
    private final HashMap<SWEKSupplier, List<SWEK.Param>> filters = new HashMap<>();
    private Consumer<SWEKGroup> groupChanged = _ -> {};
    private volatile boolean closed;
    private final Set<EventListener.Handle> cacheEventHandlers = new HashSet<>();
    private final Set<EventListener.Highlight> highlightListeners = new HashSet<>();
    private EventCollection eventCollection = new EventCollection();

    private RelatedEvents lastHighlighted = null;

    public SWEKSession(SWEKCatalog _catalog) {
        catalog = _catalog;
        database = new EventDatabase(catalog);
        downloader = new SWEKDownloader(this, database);
    }

    public SWEKCatalog catalog() {
        return catalog;
    }

    public List<SWEK.Param> getFilters(SWEKSupplier supplier) {
        return filters.getOrDefault(supplier, List.of());
    }

    public void setFilters(SWEKSupplier supplier, List<SWEK.Param> values) {
        if (closed) return;
        if (values.isEmpty()) filters.remove(supplier);
        else filters.put(supplier, List.copyOf(values));
        downloader.filtersChanged(supplier);
    }

    public boolean isSupplierActive(SWEKSupplier supplier) {
        return downloader.isSupplierActive(supplier);
    }

    public void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (!closed) downloader.setSupplierActive(supplier, active);
    }

    public void requestForInterval(long start, long end) {
        if (!closed) downloader.requestForInterval(start, end);
    }

    public boolean isGroupBusy(SWEKGroup group) {
        return downloader.isGroupBusy(group);
    }

    public void setGroupChangedCallback(Consumer<SWEKGroup> callback) {
        if (!closed) groupChanged = callback;
    }

    void groupBusyChanged(SWEKGroup group) {
        if (!closed) groupChanged.accept(group);
    }

    public EventDetails getEventDetails(int id) throws Exception {
        if (closed) throw new CancellationException("SWEK session closed");
        return database.getEventDetails(id);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        downloader.close();
        groupChanged = _ -> {};
        cacheEventHandlers.clear();
        highlightListeners.clear();
        if (lastHighlighted != null) lastHighlighted.highlight(false);
        lastHighlighted = null;
        eventCollection = new EventCollection();
        filters.clear();
        database.close();
    }

    public void registerHandler(EventListener.Handle handler) {
        if (!closed) cacheEventHandlers.add(handler);
    }

    public void unregisterHandler(EventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    void fireEventCacheChanged() {
        cacheEventHandlers.forEach(EventListener.Handle::cacheUpdated);
    }

    public void highlight(RelatedEvents event) {
        if (closed) return;
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

    public void addHighlightListener(EventListener.Highlight listener) {
        if (!closed) highlightListeners.add(listener);
    }

    public void removeHighlightListener(EventListener.Highlight listener) {
        highlightListeners.remove(listener);
    }

    private void fireHighlightChanged() {
        highlightListeners.forEach(EventListener.Highlight::highlightChanged);
    }

    void replaceEvents(EventBatch batch) {
        if (closed) return;
        boolean highlightCached = eventCollection.contains(lastHighlighted);
        eventCollection.replace(batch);
        if (highlightCached && !eventCollection.contains(lastHighlighted))
            highlight(null);
        fireEventCacheChanged();
    }

    @Nullable
    public RelatedEvents getRelatedEvents(int id) {
        return eventCollection.getRelatedEvents(id);
    }

    public List<RelatedEvents> getEvents(long start, long end) {
        return eventCollection.getEvents(start, end);
    }

    void removeSupplier(SWEKSupplier supplier) {
        if (lastHighlighted != null && lastHighlighted.getEvents().stream().anyMatch(event ->
                event.getSupplier() == supplier && eventCollection.getRelatedEvents(event.getUniqueID()) == lastHighlighted))
            highlight(null);
        eventCollection.removeSupplier(supplier);
        fireEventCacheChanged();
    }

}
