package org.helioviewer.jhv.image;

import java.awt.EventQueue;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public final class ImageBufferCache {

    public interface Key {
        Object owner();
    }

    private static final long MAX_CACHE_BYTES = 8L * 1024 * 1024 * 1024;

    private static final Cache<Key, DecodedImage> cache = createCache(MAX_CACHE_BYTES);

    static Cache<Key, DecodedImage> createCache(long maximumBytes) {
        return CacheBuilder.newBuilder()
                .concurrencyLevel(1) // One shared byte budget and LRU order, without frequency-based admission.
                .maximumWeight(maximumBytes)
                .weigher((Key key, DecodedImage value) -> value.imageBuffer().byteSize())
                .removalListener(notification -> EventQueue.invokeLater(notification.getValue()::close))
                .build();
    }

    // Borrowed during the EDT turn; retain the image before keeping it beyond that turn.
    @Nullable
    public static DecodedImage get(Key key) {
        return cache.getIfPresent(key);
    }

    // Transfers the decoder's reference to the cache.
    public static void put(Key key, DecodedImage image) {
        cache.put(key, image);
    }

    public static void invalidateIf(Predicate<Key> predicate) {
        cache.asMap().keySet().removeIf(predicate);
    }

    public static void invalidateOwners(Set<Object> owners) {
        if (!owners.isEmpty())
            invalidateIf(key -> owners.contains(key.owner()));
    }

    private ImageBufferCache() {}
}
