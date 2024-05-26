package org.helioviewer.jhv.plugins.pfss;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

class PfssCache {

    private final TreeMap<Long, URI> map = new TreeMap<>();
    private final Cache<URI, PfssLoader.Data> cache = Caffeine.newBuilder().softValues().build();

    void put(Map<Long, URI> uris) {
        map.putAll(uris);
    }

    void putData(URI uri, PfssLoader.Data data) {
        cache.put(uri, data);
    }

    private PfssLoader.Data get(long time, URI uri) {
        PfssLoader.Data ret = cache.getIfPresent(uri);
        if (ret == null) {
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
    }

}
