package org.helioviewer.jhv.image;

import java.awt.EventQueue;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

public final class ImageBufferCache {

    private static final long MAX_CACHE_BYTES = 8L * 1024 * 1024 * 1024;

    private static final Cache<Object, DecodedImage> cache = Caffeine.newBuilder()
            .maximumWeight(MAX_CACHE_BYTES)
            .weigher((Object key, DecodedImage value) -> value.imageBuffer().byteSize())
            .removalListener((Object key, DecodedImage value, RemovalCause cause) -> EventQueue.invokeLater(value::close))
            .build();

    // Borrowed during the EDT turn; retain the image before keeping it beyond that turn.
    @Nullable
    public static DecodedImage get(Object key) {
        return cache.getIfPresent(key);
    }

    // Transfers the decoder's reference to the cache.
    public static void put(Object key, DecodedImage image) {
        cache.put(key, image);
    }

    public static void invalidateIf(Predicate<Object> predicate) {
        cache.asMap().entrySet().removeIf(entry -> predicate.test(entry.getKey()));
    }

    private ImageBufferCache() {}
}
