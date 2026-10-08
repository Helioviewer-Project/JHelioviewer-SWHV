package org.helioviewer.jhv.image;

import java.awt.EventQueue;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;

import org.lwjgl.system.MemoryUtil;

import com.google.common.cache.Cache;

// Native allocation checks, without drawing or relying on garbage collection.
public final class ImageBufferCacheTest {

    public static void main(String[] arguments) throws Exception {
        releasedImage();
        manyFiles();
        explicitRemoval();
        heldAndQueuedImages();
        replacementAndEviction();
        weightedLRU();
        resolutionChange();
        concurrentReplacement();
        System.out.println("PASS: release checks without assertions, weighted LRU, resolution changes, removal, held and queued images, replacement and concurrent invalidation without repainting");
    }

    private record OwnedKey(Object id, int frame, AtomicInteger visits) implements ImageBufferCache.Key {
        @Override
        public Object owner() {
            visits.incrementAndGet();
            return id;
        }
    }

    private static void manyFiles() throws Exception {
        int count = 2000;
        CountDownLatch closed = new CountDownLatch(count);
        Set<Thread> closerThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger visits = new AtomicInteger(), handlers = new AtomicInteger();
        List<View> views = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long date = i * 3L;
            Object id = new Object();
            views.add(new View() {
                @Override public int getMaximumFrameNumber() { return 2; }
                @Override public JHVTime getFrameTime(int frame) { return new JHVTime(date + frame); }
                @Override public JHVTime getFirstTime() { return getFrameTime(0); }
                @Override public JHVTime getLastTime() { return getFrameTime(2); }
                @Override public boolean setNearestFrame(JHVTime time) { return true; }
                @Override public JHVTime getNearestTime(JHVTime time) { return time; }
                @Override public JHVTime getLowerTime(JHVTime time) { return time; }
                @Override public JHVTime getHigherTime(JHVTime time) { return time; }
                @Override public MetaData getMetaData(JHVTime time) { throw new UnsupportedOperationException(); }
                @Override public void collectCacheOwners(Set<Object> owners) { owners.add(id); }
                @Override public void setDataHandler(View.DataHandler handler) { handlers.incrementAndGet(); }
                @Override public void closeSources() {
                    if (EventQueue.isDispatchThread())
                        throw new AssertionError("Sources closed on the EDT");
                    closerThreads.add(Thread.currentThread());
                    closed.countDown();
                }
            });
            for (int frame = 0; frame < 3; frame++)
                ImageBufferCache.put(new OwnedKey(id, frame, visits),
                        new DecodedImage(ImageBuffer.fromBytes(8, 8, ImageBuffer.Format.Gray8, new byte[64]), null));
        }
        OwnedKey other = new OwnedKey(new Object(), 0, visits);
        DecodedImage retained = image();
        ImageBufferCache.put(other, retained);
        ManyView collection = new ManyView(List.of(new ManyView(views.subList(0, count / 2)),
                new ManyView(views.subList(count / 2, count))));
        try {
            EventQueue.invokeAndWait(() -> {
                collection.setDataHandler(null);
                collection.clearCache();
            });
            if (visits.get() != count * 3 + 1 || handlers.get() != count || ImageBufferCache.get(other) != retained)
                throw new AssertionError("Collection did not visit each cache entry and child once, or purged another layer");
            visits.set(0);
            EventQueue.invokeAndWait(collection::abolish);
            if (!closed.await(10, TimeUnit.SECONDS) || closerThreads.size() != 1 || visits.get() != 1)
                throw new AssertionError("Collection repeated cache scans or used multiple source-close threads");
        } finally {
            ImageBufferCache.invalidateIf(key -> key instanceof OwnedKey);
        }
        System.out.println("PASS: 2,000 views and 6,000 frames use one cache pass and one background source closer");
    }

    private static DecodedImage image() {
        return new DecodedImage(ImageBuffer.fromBytes(256, 256, ImageBuffer.Format.Gray8, new byte[256 * 256]), null);
    }

    private static void releasedImage() throws Exception {
        DecodedImage image = image();
        EventQueue.invokeAndWait(() -> {
            image.close();
            try {
                image.retain();
                throw new AssertionError("Retained a released image");
            } catch (IllegalStateException expected) {
            }
            try {
                image.close();
                throw new AssertionError("Released an image twice");
            } catch (IllegalStateException expected) {
            }
        });
        if (allocated(image))
            throw new AssertionError("Released image remains allocated");
    }

    private static boolean allocated(DecodedImage image) {
        long address = MemoryUtil.memAddress((ByteBuffer) image.imageBuffer().buffer);
        AtomicBoolean found = new AtomicBoolean();
        MemoryUtil.memReport((allocation, bytes, thread, name, stack) -> {
            if (allocation == address)
                found.set(true);
        });
        return found.get();
    }

    private static void await(BooleanSupplier done, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!done.getAsBoolean()) {
            if (System.nanoTime() >= deadline)
                throw new AssertionError(message);
            EventQueue.invokeAndWait(() -> {});
            Thread.sleep(5);
        }
    }

    private static void explicitRemoval() throws Exception {
        DecodedImage[] images = {image(), image(), image()};
        for (DecodedImage image : images) {
            if (!allocated(image))
                throw new AssertionError("Native allocator tracking is not enabled");
            ImageBufferCache.put(image, image);
        }
        EventQueue.invokeAndWait(() -> ImageBufferCache.invalidateIf(key -> key instanceof DecodedImage));
        for (DecodedImage image : images) {
            await(() -> !allocated(image), "Removed image required repainting or garbage collection");
            if (ImageBufferCache.get(image) != null)
                throw new AssertionError("Removed image remains cached");
        }
    }

    private static void heldAndQueuedImages() throws Exception {
        DecodedImage shared = image(), queued = image();
        EventQueue.invokeAndWait(() -> {
            // All three layer slots may hold the same image; two deliveries may also be queued.
            for (int slot = 0; slot < 3; slot++)
                shared.retain();
            queued.retain();
            queued.retain();
            ImageBufferCache.put(shared, shared);
            ImageBufferCache.put(queued, queued);
            ImageBufferCache.invalidateIf(key -> key instanceof DecodedImage);
        });
        EventQueue.invokeAndWait(() -> {});
        if (!allocated(shared) || !allocated(queued))
            throw new AssertionError("A held or queued image was freed");
        EventQueue.invokeAndWait(() -> {
            shared.close();
            shared.close();
            queued.close();
        });
        if (!allocated(shared) || !allocated(queued))
            throw new AssertionError("An image with a remaining owner was freed");
        EventQueue.invokeAndWait(() -> {
            shared.close();
            queued.close();
        });
        await(() -> !allocated(shared) && !allocated(queued), "Last release did not free the images");
    }

    private static void replacementAndEviction() throws Exception {
        Object key = new Object();
        DecodedImage first = image(), second = image();
        ImageBufferCache.put(key, first);
        ImageBufferCache.put(key, second);
        await(() -> !allocated(first), "Replaced image was not freed");
        if (ImageBufferCache.get(key) != second || !allocated(second))
            throw new AssertionError("Replacement image was freed");
        ImageBufferCache.invalidateIf(candidate -> candidate == key);
        await(() -> !allocated(second), "Replacement image was not freed on removal");
        Cache<Object, DecodedImage> cache = ImageBufferCache.createCache(1);
        DecodedImage third = image();
        cache.put(key, third);
        await(() -> !allocated(third), "Oversized image was not freed");
        if (cache.getIfPresent(key) != null)
            throw new AssertionError("Oversized image remains cached");
    }

    private static void weightedLRU() throws Exception {
        DecodedImage first = image(), second = image(), third = image();
        DecodedImage large = new DecodedImage(ImageBuffer.fromBytes(512, 256, ImageBuffer.Format.Gray8, new byte[512 * 256]), null);
        Cache<Object, DecodedImage> cache = ImageBufferCache.createCache(3L * first.imageBuffer().byteSize());
        EventQueue.invokeAndWait(() -> {
            cache.put(0, first);
            cache.put(1, second);
            cache.put(2, third);
            // A frequently used image is still evicted once it is the least recently used.
            for (int repeat = 0; repeat < 100; repeat++)
                cache.getIfPresent(0);
            cache.getIfPresent(1);
            cache.getIfPresent(2);
            first.retain(); // A layer may continue displaying an evicted image.
            cache.put(3, large);
            if (cache.getIfPresent(0) != null || cache.getIfPresent(1) != null
                    || cache.getIfPresent(2) != third || cache.getIfPresent(3) != large)
                throw new AssertionError("Eviction did not follow recency and byte weight");
        });
        await(() -> !allocated(second), "LRU eviction did not free the unheld image");
        if (!allocated(first))
            throw new AssertionError("LRU eviction freed a layer-owned image");
        EventQueue.invokeAndWait(first::close);
        await(() -> !allocated(first), "Last layer release did not free the evicted image");
        cache.invalidateAll();
        await(() -> !allocated(third) && !allocated(large), "LRU cache removal did not free images");
    }

    private static void resolutionChange() throws Exception {
        // Scale the images and budget equally: 512 old frames fill the cache, as at 8 GiB with 4K grayscale.
        for (int side : new int[]{256, 128}) {
            Cache<Object, DecodedImage> cache = ImageBufferCache.createCache(512L * 256 * 256);
            ArrayList<DecodedImage> images = new ArrayList<>();
            EventQueue.invokeAndWait(() -> {
                for (int frame = 0; frame < 512; frame++) {
                    DecodedImage image = image();
                    images.add(image);
                    cache.put("old-" + frame, image);
                }
                for (int loop = 0; loop < 20; loop++) {
                    for (int frame = 0; frame < 512; frame++)
                        cache.getIfPresent("old-" + frame);
                }
                for (int loop = 0; loop < 3; loop++) {
                    int misses = 0;
                    for (int frame = 0; frame < 512; frame++) {
                        String key = "new-" + frame;
                        if (cache.getIfPresent(key) == null) {
                            misses++;
                            DecodedImage image = new DecodedImage(ImageBuffer.fromBytes(side, side, ImageBuffer.Format.Gray8, new byte[side * side]), null);
                            images.add(image);
                            cache.put(key, image);
                        }
                    }
                    if (misses != (loop == 0 ? 512 : 0))
                        throw new AssertionError("Resolution change caused " + misses + " decodes on loop " + loop);
                }
                cache.invalidateAll();
            });
            await(() -> images.stream().noneMatch(ImageBufferCacheTest::allocated), "Resolution-change images were not freed");
        }
    }

    private static void concurrentReplacement() throws Exception {
        Object key = new Object();
        DecodedImage first = image(), second = image();
        ImageBufferCache.put(key, first);
        CountDownLatch inspected = new CountDownLatch(1), replace = new CountDownLatch(1);
        Thread invalidator = new Thread(() -> ImageBufferCache.invalidateIf(candidate -> {
            if (candidate != key)
                return false;
            inspected.countDown();
            try {
                replace.await();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
            return true;
        }));
        invalidator.start();
        try {
            if (!inspected.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Invalidation did not inspect the image");
            ImageBufferCache.put(key, second);
        } finally {
            replace.countDown();
        }
        invalidator.join(10000);
        if (invalidator.isAlive())
            throw new AssertionError("Invalidation did not finish");
        await(() -> !allocated(first), "Replaced image was not freed during invalidation");
        if (ImageBufferCache.get(key) != second || !allocated(second))
            throw new AssertionError("Concurrent replacement was removed or freed");
        ImageBufferCache.invalidateIf(candidate -> candidate == key);
        await(() -> !allocated(second), "Final image was not freed");
    }

}
