package org.helioviewer.jhv.imagedata;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public final class ImageBufferCache {

    private static final long MAX_CACHE_BYTES = 8L * 1024 * 1024 * 1024;

    private static final Cache<Object, ImageBuffer> cache = Caffeine.newBuilder()
            .maximumWeight(MAX_CACHE_BYTES)
            .weigher((Object key, ImageBuffer value) -> value.byteSize())
            .build();

    private ImageBufferCache() {
    }

    @Nullable
    public static ImageBuffer get(Object key) {
        return cache.getIfPresent(key);
    }

    public static void put(Object key, ImageBuffer imageBuffer) {
        cache.put(key, imageBuffer);
    }

    public static void invalidate(Object key) {
        cache.invalidate(key);
    }

    public static void invalidateIf(Predicate<Object> predicate) {
        cache.asMap().keySet().removeIf(predicate);
    }
}
