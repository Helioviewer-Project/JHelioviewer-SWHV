package org.helioviewer.jhv.image;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

public final class ImageBufferCache {

    private static final long MAX_CACHE_BYTES = 8L * 1024 * 1024 * 1024;
    private static final ArrayList<WeakReference<ImageBuffer>> retired = new ArrayList<>();

    private static final Cache<Object, DecodedImage> cache = Caffeine.newBuilder()
            .maximumWeight(MAX_CACHE_BYTES)
            .weigher((Object key, DecodedImage value) -> value.imageBuffer().byteSize())
            .removalListener((Object key, DecodedImage value, RemovalCause cause) -> retire(value.imageBuffer()))
            .build();

    @Nullable
    public static DecodedImage get(Object key) {
        return cache.getIfPresent(key);
    }

    public static void put(Object key, DecodedImage image) {
        cache.put(key, image);
    }

    public static void invalidateIf(Predicate<Object> predicate) {
        cache.asMap().keySet().removeIf(predicate);
    }

    private static void retire(ImageBuffer imageBuffer) { // we are using strong values.
        synchronized (retired) {
            retired.add(new WeakReference<>(imageBuffer));
        }
    }

    public static void reap(Set<ImageBuffer> retained) {
        cache.cleanUp();
        synchronized (retired) {
            if (retired.isEmpty())
                return;
            Iterator<WeakReference<ImageBuffer>> iterator = retired.iterator();
            while (iterator.hasNext()) {
                ImageBuffer imageBuffer = iterator.next().get();
                if (imageBuffer == null || (!retained.contains(imageBuffer) && imageBuffer.free()))
                    iterator.remove();
            }
        }
    }

    private ImageBufferCache() {}
}
