package org.helioviewer.jhv.plugins.pfss.data;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class PfssCache {

    private final TreeMap<Long, URI> map = new TreeMap<>();
    private final Cache<URI, PfssData> cache = Caffeine.newBuilder().softValues().build();

    void put(Map<Long, URI> uris) {
        map.putAll(uris);
    }

    void putData(URI uri, PfssData data) {
        cache.put(uri, data);
    }

    private PfssData get(long time, URI uri) {
        PfssData ret = cache.getIfPresent(uri);
        if (ret == null) {
            PfssLoader.submit(time, uri);
        }
        return ret;
    }

    @Nullable
    public PfssData getNearestData(long time) {
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

    public void clear() {
        map.clear();
        cache.invalidateAll();
    }

}
