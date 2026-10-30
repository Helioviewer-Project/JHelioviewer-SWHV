package org.helioviewer.jhv.plugins.pfss;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.thread.Task;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

class PfssCache {

    private final TreeMap<Long, URI> map = new TreeMap<>();
    private final Cache<URI, PfssLoader.Data> cache = Caffeine.newBuilder().softValues().build();
    private final Set<URI> inFlight = new HashSet<>();
    private int listDownloads;

    void submitList(long start, long end) {
        listDownloads++;
        Task.submitBackground(new PfssLoader.ListLoader(start, end, map::putAll), result -> {
            listDownloads--;
            getNearestData(start);
        }, t -> {
            listDownloads--;
            Log.error(t);
        });
    }

    boolean isDownloading() {
        return listDownloads != 0 || !inFlight.isEmpty();
    }

    private PfssLoader.Data get(long time, URI uri) {
        PfssLoader.Data ret = cache.getIfPresent(uri);
        if (ret == null && inFlight.add(uri)) {
            Task.submitBackground(new PfssLoader.DataLoader(time, uri), data -> {
                cache.put(uri, data);
                inFlight.remove(uri);
                DisplayController.display();
            }, t -> {
                inFlight.remove(uri);
                Log.error(t);
            });
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
