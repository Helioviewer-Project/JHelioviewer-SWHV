package org.helioviewer.jhv.plugins.pfss.data;

import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.plugins.pfss.PfssPlugin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class PfssCache {

    private final TreeMap<Long, String> map = new TreeMap<>();
    private final Cache<String, PfssData> cache = CacheBuilder.newBuilder().softValues().build();

    void put(Map<Long, String> urls) {
        map.putAll(urls);
    }

    void putData(String url, PfssData data) {
        cache.put(url, data);
    }

    private PfssData get(long time, String url) {
        PfssData ret = cache.getIfPresent(url);
        if (ret == null) {
            PfssPlugin.pfssDataPool.execute(new PfssDataLoader(time, url));
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
