package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

public class JHVEventCache {

    private static final double FACTOR = 0.2;
    private static final long REQUEST_REFRESH_INTERVAL = 60 * 60 * 1000L;
    private static final long FUTURE_REQUEST_MARGIN = 6 * REQUEST_REFRESH_INTERVAL;

    private static final Set<JHVEventListener.Handle> cacheEventHandlers = new HashSet<>();
    private static final Set<JHVEventListener.Highlight> highlightListeners = new HashSet<>();
    private static final JHVEventGroups eventGroups = new JHVEventGroups();
    private static final Map<SWEKSupplier, RequestCache> requestedIntervals = new HashMap<>();

    private static JHVRelatedEvents lastHighlighted = null;

    public static void registerHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.add(handler);
    }

    public static void unregisterHandler(JHVEventListener.Handle handler) {
        cacheEventHandlers.remove(handler);
    }

    static void fireEventCacheChanged() {
        cacheEventHandlers.forEach(JHVEventListener.Handle::cacheUpdated);
    }

    static void requestFailed(SWEKSupplier eventType, long start, long end) {
        RequestCache cache = requestedIntervals.get(eventType);
        if (cache != null)
            cache.removeRequestedInterval(start, end);
    }

    public static boolean isSupplierActive(SWEKSupplier supplier) {
        return requestedIntervals.containsKey(supplier);
    }

    public static void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (active) {
            requestedIntervals.computeIfAbsent(supplier, _ -> new RequestCache());
            fireEventCacheChanged();
        } else {
            SWEKDownloader.stopDownloadSupplier(supplier, false);
        }
    }

    public static void highlight(JHVRelatedEvents event) {
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

    public static void addHighlightListener(JHVEventListener.Highlight listener) {
        highlightListeners.add(listener);
    }

    public static void removeHighlightListener(JHVEventListener.Highlight listener) {
        highlightListeners.remove(listener);
    }

    private static void fireHighlightChanged() {
        highlightListeners.forEach(JHVEventListener.Highlight::highlightChanged);
        DisplayController.display();
    }

    static void addEvent(JHVEvent event) {
        eventGroups.addEvent(event);
    }

    static void addAssociation(JHVEvent.Link link) {
        eventGroups.addAssociation(link);
    }

    @Nullable
    public static JHVRelatedEvents getRelatedEvents(int id) {
        return eventGroups.getRelatedEvents(id);
    }

    public static List<JHVRelatedEvents> getEvents(long start, long end) {
        return eventGroups.getEvents(start, end);
    }

    public static void requestForInterval(long start, long end) {
        long now = System.currentTimeMillis();
        long requestHorizon = now / REQUEST_REFRESH_INTERVAL * REQUEST_REFRESH_INTERVAL + FUTURE_REQUEST_MARGIN;
        long visibleEnd = Math.min(end, requestHorizon);
        if (start >= visibleEnd)
            return;

        long deltaT = Math.max((long) ((visibleEnd - start) * FACTOR), TimeUtils.DAY_IN_MILLIS);
        long requestStart = start - deltaT;
        long requestEnd = Math.min(visibleEnd + deltaT, requestHorizon);
        for (Map.Entry<SWEKSupplier, RequestCache> entry : requestedIntervals.entrySet()) {
            RequestCache cache = entry.getValue();
            if (!cache.getMissingIntervals(start, visibleEnd).isEmpty())
                SWEKDownloader.startDownloadSupplier(entry.getKey(), cache.adaptRequestCache(requestStart, requestEnd));
        }
    }

    static void removeSupplier(SWEKSupplier supplier, boolean keepActive) {
        if (keepActive)
            requestedIntervals.put(supplier, new RequestCache());
        else
            requestedIntervals.remove(supplier);
        if (lastHighlighted != null && lastHighlighted.getEvents().stream().anyMatch(event ->
                event.getSupplier() == supplier && eventGroups.getRelatedEvents(event.getUniqueID()) == lastHighlighted))
            highlight(null);
        eventGroups.removeSupplier(supplier);
        fireEventCacheChanged();
    }

}
