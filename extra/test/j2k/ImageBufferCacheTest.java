package org.helioviewer.jhv.image;

import java.awt.EventQueue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.lwjgl.system.MemoryUtil;

import com.github.benmanes.caffeine.cache.Cache;

// Native allocation checks, without drawing or relying on garbage collection.
public final class ImageBufferCacheTest {

    public static void main(String[] arguments) throws Exception {
        explicitRemoval();
        heldAndQueuedImages();
        replacementAndEviction();
        concurrentReplacement();
        System.out.println("PASS: removal, held and queued images, replacement, eviction and concurrent invalidation without repainting");
    }

    private static DecodedImage image() {
        return new DecodedImage(ImageBuffer.fromBytes(256, 256, ImageBuffer.Format.Gray8, new byte[256 * 256]), null);
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
        Cache<Object, DecodedImage> cache = cache();
        long maximum = cache.policy().eviction().orElseThrow().getMaximum();
        try {
            cache.policy().eviction().orElseThrow().setMaximum(1);
            await(() -> !allocated(second), "Evicted image was not freed");
            DecodedImage third = image();
            ImageBufferCache.put(new Object(), third);
            await(() -> !allocated(third), "Automatic eviction did not free the inserted image");
        } finally {
            cache.policy().eviction().orElseThrow().setMaximum(maximum);
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

    @SuppressWarnings("unchecked")
    private static Cache<Object, DecodedImage> cache() throws Exception {
        Field field = ImageBufferCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        return (Cache<Object, DecodedImage>) field.get(null);
    }

}
