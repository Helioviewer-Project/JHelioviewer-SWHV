package org.helioviewer.jhv.plugins.pfss;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

class PfssCache {

    private final TreeMap<Long, URI> map = new TreeMap<>();
    private final Cache<URI, PfssLoader.Data> cache = Caffeine.newBuilder().softValues().build();
    private final Set<URI> inFlight = ConcurrentHashMap.newKeySet();
    private final AtomicInteger downloads = new AtomicInteger();

    void beginDownload() {
        downloads.incrementAndGet();
    }

    void endDownload() {
        downloads.updateAndGet(value -> Math.max(0, value - 1));
    }

    boolean isDownloading() {
        return downloads.get() != 0;
    }

    void put(Map<Long, URI> uris) {
        map.putAll(uris);
    }

    void markLoaded(URI uri, PfssLoader.Data data) {
        cache.put(uri, data);
        inFlight.remove(uri);
    }

    void markFailed(URI uri) {
        inFlight.remove(uri);
    }

    private PfssLoader.Data get(long time, URI uri) {
        PfssLoader.Data ret = cache.getIfPresent(uri);
        if (ret == null && inFlight.add(uri)) {
            PfssLoader.submitData(time, uri);
        }
        return ret;
    }

    @Nullable
    PfssLoader.Data getNearestData(long time) {
        Long c = map.ceilingKey(time);
        Long f = map.floorKey(time);

        if (f != null && c != null)
            return time - f < c - time ? get(f, map.get(f)) : get(c, map.get(c));
        if (f == null && c != null)
            return get(c, map.get(c));
        if (f != null)
            return get(f, map.get(f));
        return null;
    }

    void clear() {
        map.clear();
        cache.invalidateAll();
        inFlight.clear();
        downloads.set(0);
    }

}
