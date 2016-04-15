package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;

public class EVECacheController {

    private static final EVECacheController singletonInstance = new EVECacheController();

    private final LinkedList<EVECacheControllerListener> controllerListeners = new LinkedList<EVECacheControllerListener>();

    private final EVEBandCache cache = new EVEBandCache();

    private final Map<Band, RequestCache> requestCache = new HashMap<Band, RequestCache>();

    private EVECacheController() {
    }

    public static EVECacheController getSingletonInstance() {
        return singletonInstance;
    }

    public void addControllerListener(final EVECacheControllerListener listener) {
        controllerListeners.add(listener);
    }

    public void removeControllerListener(final EVECacheControllerListener listener) {
        controllerListeners.remove(listener);
    }

    public void addToCache(Band band, float[] values, long[] dates) {
        cache.add(band, values, dates);
        fireDataAdded(band);
    }

    public List<Interval> addRequest(Band band, Interval interval) {
        RequestCache rc = getRequestCache(band);
        return rc.adaptRequestCache(interval.start, interval.end);
    }

    public List<Interval> getMissingDaysInInterval(final Band band, final Interval interval) {
        RequestCache rc = getRequestCache(band);
        return rc.getMissingIntervals(interval);
    }

    private RequestCache getRequestCache(Band band) {
        RequestCache rc = requestCache.get(band);
        if (rc == null) {
            requestCache.put(band, new RequestCache());
        }
        return requestCache.get(band);
    }

    private void fireDataAdded(final Band band) {
        for (EVECacheControllerListener listener : controllerListeners) {
            listener.dataAdded(band);
        }
    }

    public EVEValues downloadData(Band band, Interval interval, Rectangle plotArea) {
        return cache.getValuesInInterval(band, interval, plotArea);
    }

    public boolean hasDataInSelectedInterval(Band band, Interval selectedInterval) {
        return cache.hasDataInInterval(band, selectedInterval);
    }

}
