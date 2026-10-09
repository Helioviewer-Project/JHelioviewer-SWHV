package org.helioviewer.jhv.image;

import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.layers.Frames;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.time.JHVTime;

import org.lwjgl.system.MemoryUtil;

import com.google.common.cache.Cache;

// Native allocation checks, without drawing or relying on garbage collection.
public final class ImageBufferCacheTest {

    public static void main(String[] arguments) throws Exception {
        decodeRequests();
        releasedImage();
        writeBuffers();
        manyFiles();
        explicitRemoval();
        heldAndQueuedImages();
        replacementAndEviction();
        weightedLRU();
        resolutionChange();
        System.out.println("PASS: release checks without assertions, weighted LRU, resolution changes, removal, held and queued images and replacement without repainting");
    }

    private record TestKey(Object owner, int frame) implements ImageBufferCache.Key {}

    private static void decodeRequests() throws Exception {
        ArrayBlockingQueue<String> delivered = new ArrayBlockingQueue<>(16);
        AtomicInteger executions = new AtomicInteger();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            LatestWorker<Integer> worker = new LatestWorker<>(executor);
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
            Callable<Integer> blocked = () -> {
                int count = executions.incrementAndGet();
                started.countDown();
                if (!release.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Decode was not released");
                return count;
            };
            try {
                worker.submit("a", blocked, decodeCallback(delivered, "original"));
                if (!started.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Decode did not start");
                worker.submit("a", executions::incrementAndGet, decodeCallback(delivered, "repeat"));
                worker.submit("b", executions::incrementAndGet, decodeCallback(delivered, "superseded"));
                worker.submit("a", executions::incrementAndGet, decodeCallback(delivered, "latest viewpoint"));
                release.countDown();
                awaitDecode(delivered, "latest viewpoint:true");
                if (executions.get() != 1)
                    throw new AssertionError("Repeated key or A-B-A decoded again");

                // Completion must forget the key, so an evicted image can be decoded again.
                worker.submit("a", executions::incrementAndGet, decodeCallback(delivered, "after eviction"));
                awaitDecode(delivered, "after eviction:true");

                EventQueue.invokeAndWait(() -> {
                    worker.submit("queued", executions::incrementAndGet, decodeCallback(delivered, "old queued"));
                    finishTasks(executor); // Decode finished, but its callback cannot run in this EDT turn.
                    worker.submit("queued", executions::incrementAndGet, decodeCallback(delivered, "repeat queued"));
                    worker.submit("other", executions::incrementAndGet, decodeCallback(delivered, "other"));
                    finishTasks(executor); // Different work continues while publication waits for the EDT.
                    worker.submit("queued", executions::incrementAndGet, decodeCallback(delivered, "latest queued"));
                });
                awaitDecode(delivered, "latest queued:true");
                awaitDecode(delivered, "other:false");
                if (executions.get() != 4)
                    throw new AssertionError("Result waiting for the EDT was decoded again");

                worker.submit("failure", () -> {
                    executions.incrementAndGet();
                    throw new IllegalStateException("Decode failure");
                }, decodeCallback(delivered, "failure"));
                awaitDecode(delivered, "failure:failed:true");
                worker.submit("failure", executions::incrementAndGet, decodeCallback(delivered, "retry"));
                awaitDecode(delivered, "retry:true");

                EventQueue.invokeAndWait(() -> {
                    worker.submit("invalidated", executions::incrementAndGet, decodeCallback(delivered, "invalidated"));
                    finishTasks(executor);
                    worker.invalidate();
                    worker.submit("invalidated", executions::incrementAndGet, decodeCallback(delivered, "after invalidation"));
                });
                awaitDecode(delivered, "invalidated:false");
                awaitDecode(delivered, "after invalidation:true");

                EventQueue.invokeAndWait(() -> {
                    worker.submit(executions::incrementAndGet, decodeCallback(delivered, "unkeyed first"));
                    finishTasks(executor);
                    worker.submit(executions::incrementAndGet, decodeCallback(delivered, "unkeyed latest"));
                });
                awaitDecode(delivered, "unkeyed first:false");
                awaitDecode(delivered, "unkeyed latest:true");

                worker.dispose();
                try {
                    worker.submit("disposed", executions::incrementAndGet, decodeCallback(delivered, "disposed"));
                    throw new AssertionError("Disposed worker accepted a decode");
                } catch (RejectedExecutionException expected) {
                }
                if (executions.get() != 10 || !delivered.isEmpty())
                    throw new AssertionError("Unexpected decode or callback count");
            } finally {
                release.countDown();
                worker.dispose();
            }
        }
        System.out.println("PASS: keyed running and queued decodes coalesce with the latest callback, A-B-A, retry, invalidation and unkeyed submission");
    }

    private static void finishTasks(ExecutorService executor) {
        try {
            // The previous task can schedule pending work behind the first fence.
            for (int pass = 0; pass < 2; pass++)
                executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Decode worker did not finish independently of the EDT", e);
        }
    }

    private static LatestWorker.Callback<Integer> decodeCallback(ArrayBlockingQueue<String> delivered, String context) {
        return new LatestWorker.Callback<>() {
            @Override
            public void onSuccess(Integer result, boolean fresh) {
                delivered.add(context + ":" + fresh);
            }

            @Override
            public void onFailure(Throwable failure, boolean fresh) {
                delivered.add(context + ":failed:" + fresh);
            }
        };
    }

    private static void awaitDecode(ArrayBlockingQueue<String> delivered, String expected) throws Exception {
        String actual = delivered.poll(5, TimeUnit.SECONDS);
        if (!expected.equals(actual))
            throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private record OwnedKey(Object id, int frame, AtomicInteger visits) implements ImageBufferCache.Key {
        @Override
        public Object owner() {
            visits.incrementAndGet();
            return id;
        }
    }

    private static void manyFiles() throws Exception {
        int count = 1000;
        CountDownLatch closed = new CountDownLatch(count);
        Set<Thread> closerThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger visits = new AtomicInteger(), listeners = new AtomicInteger();
        List<Frames.Frame> frames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long date = i * 3L;
            Source source = new Source() {
                @Override public int frames() { return 3; }
                @Override public String xml(int frame) { return null; }
                @Override public ResolutionSet levels(int frame) { throw new UnsupportedOperationException(); }
                @Override public LUT lut() { return null; }
                @Override public ClipSet clipSet() { return null; }
                @Override public boolean usesFITSParameters() { return false; }
                @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) {
                    throw new UnsupportedOperationException();
                }
                @Override public void setListener(Source.Listener listener) { listeners.incrementAndGet(); }
                @Override public void close() {
                    if (EventQueue.isDispatchThread())
                        throw new AssertionError("Sources closed on the EDT");
                    closerThreads.add(Thread.currentThread());
                    closed.countDown();
                }
            };
            for (int frame = 0; frame < 3; frame++) {
                JHVTime time = new JHVTime(date + frame);
                frames.add(new Frames.Frame(time, null, Frames.EMPTY_METAXML, source, frame)); // metadata unused
            }
        }
        Object id = new Object();
        for (int i = 0; i < count * 3; i++)
            ImageBufferCache.put(new OwnedKey(id, i, visits),
                    new DecodedImage(ImageBuffer.fromBytes(8, 8, ImageBuffer.Format.Gray8, new byte[64]), null));
        OwnedKey other = new OwnedKey(new Object(), 0, visits);
        DecodedImage retained = image();
        ImageBufferCache.put(other, retained);
        Frames timeline = new Frames(frames);
        try {
            EventQueue.invokeAndWait(timeline::clearCache);
            if (visits.get() != count * 3 + 1 || listeners.get() != count || ImageBufferCache.get(other) != retained)
                throw new AssertionError("Timeline did not visit each cache entry once and each source once, or purged another layer");
            visits.set(0);
            EventQueue.invokeAndWait(timeline::close);
            if (!closed.await(10, TimeUnit.SECONDS) || closerThreads.size() != 1 || visits.get() != count * 3 + 1)
                throw new AssertionError("Timeline repeated cache scans or used multiple source-close threads");
        } finally {
            ImageBufferCache.invalidateIf(key -> key instanceof OwnedKey);
        }
        System.out.println("PASS: 1,000 sources in one timeline use one cache pass per clear and one background source closer");
    }

    private static DecodedImage image() {
        return new DecodedImage(ImageBuffer.fromBytes(256, 256, ImageBuffer.Format.Gray8, new byte[256 * 256]), null);
    }

    private static void writeBuffers() throws Exception {
        for (ImageBuffer.Format format : ImageBuffer.Format.values()) {
            DecodedImage result;
            try (ImageBuffer.WriteBuffer writer = ImageBuffer.createWriteBuffer(13, 11, format)) {
                if (format == ImageBuffer.Format.Gray16F) {
                    if (!writer.shortBuffer().isDirect())
                        throw new AssertionError("Writer did not allocate native pixels");
                    writer.shortBuffer().put(Float.floatToFloat16(.5f));
                } else {
                    if (!writer.byteBuffer().isDirect())
                        throw new AssertionError("Writer did not allocate native pixels");
                    writer.byteBuffer().put((byte) 12);
                }
                writer.clearPixels();
                result = new DecodedImage(writer.finish(), null);
            }
            if (!allocated(result) || result.imageBuffer().buffer.position() != 0)
                throw new AssertionError("Closing a finished writer released its image or left its position advanced");
            if (format == ImageBuffer.Format.Gray16F) {
                for (int i = 0; i < 143; i++)
                    if (((ShortBuffer) result.imageBuffer().buffer).get(i) != 0)
                        throw new AssertionError("Writer did not clear all pixels");
            } else {
                for (int i = 0; i < result.imageBuffer().byteSize(); i++)
                    if (((ByteBuffer) result.imageBuffer().buffer).get(i) != 0)
                        throw new AssertionError("Writer did not clear all pixels");
            }
            result.release();
            if (allocated(result))
                throw new AssertionError("Transferred writer pixels were not released");
        }

        Constructor<ImageFilter> constructor = ImageFilter.class.getDeclaredConstructor(ImageFilter.Algorithm.class);
        constructor.setAccessible(true);
        ImageFilter failing = constructor.newInstance((ImageFilter.Algorithm) (pixels, width, height) -> {
            throw new IllegalStateException("Filter failure");
        });
        long address;
        try (ImageBuffer.WriteBuffer writer = ImageBuffer.createWriteBuffer(13, 11, ImageBuffer.Format.Gray8)) {
            address = MemoryUtil.memAddress(writer.byteBuffer());
            try {
                writer.clearPixels().finish(failing);
                throw new AssertionError("Filter failure was ignored");
            } catch (IllegalStateException expected) {
                if (!"Filter failure".equals(expected.getMessage()))
                    throw expected;
            }
        }
        MemoryUtil.memReport((allocation, bytes, thread, name, stack) -> {
            if (allocation == address)
                throw new AssertionError("Failed filter retained its native input");
        });
        System.out.println("PASS: native writers transfer ownership, clear pixels and release input on filter failure");
    }

    private static void releasedImage() throws Exception {
        DecodedImage image = image();
        EventQueue.invokeAndWait(() -> {
            image.release();
            try {
                image.retain();
                throw new AssertionError("Retained a released image");
            } catch (IllegalStateException expected) {
            }
            try {
                image.release();
                throw new AssertionError("Released an image twice");
            } catch (IllegalStateException expected) {
            }
        });
        if (allocated(image))
            throw new AssertionError("Released image remains allocated");
    }

    private static boolean allocated(DecodedImage image) {
        long address = MemoryUtil.memAddress(image.imageBuffer().buffer);
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
            ImageBufferCache.put(new TestKey(image, 0), image);
        }
        EventQueue.invokeAndWait(() -> ImageBufferCache.invalidateIf(key -> key instanceof TestKey));
        for (DecodedImage image : images) {
            await(() -> !allocated(image), "Removed image required repainting or garbage collection");
            if (ImageBufferCache.get(new TestKey(image, 0)) != null)
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
            ImageBufferCache.put(new TestKey(shared, 0), shared);
            ImageBufferCache.put(new TestKey(queued, 0), queued);
            ImageBufferCache.invalidateIf(key -> key instanceof TestKey);
        });
        EventQueue.invokeAndWait(() -> {});
        if (!allocated(shared) || !allocated(queued))
            throw new AssertionError("A held or queued image was freed");
        EventQueue.invokeAndWait(() -> {
            shared.release();
            shared.release();
            queued.release();
        });
        if (!allocated(shared) || !allocated(queued))
            throw new AssertionError("An image with a remaining owner was freed");
        EventQueue.invokeAndWait(() -> {
            shared.release();
            queued.release();
        });
        await(() -> !allocated(shared) && !allocated(queued), "Last release did not free the images");
    }

    private static void replacementAndEviction() throws Exception {
        TestKey key = new TestKey(new Object(), 0);
        DecodedImage first = image(), second = image();
        ImageBufferCache.put(key, first);
        ImageBufferCache.put(key, second);
        await(() -> !allocated(first), "Replaced image was not freed");
        if (ImageBufferCache.get(key) != second || !allocated(second))
            throw new AssertionError("Replacement image was freed");
        ImageBufferCache.invalidateIf(candidate -> candidate == key);
        await(() -> !allocated(second), "Replacement image was not freed on removal");
        Cache<ImageBufferCache.Key, DecodedImage> cache = ImageBufferCache.createCache(1);
        DecodedImage third = image();
        cache.put(key, third);
        await(() -> !allocated(third), "Oversized image was not freed");
        if (cache.getIfPresent(key) != null)
            throw new AssertionError("Oversized image remains cached");
    }

    private static void weightedLRU() throws Exception {
        DecodedImage first = image(), second = image(), third = image();
        DecodedImage large = new DecodedImage(ImageBuffer.fromBytes(512, 256, ImageBuffer.Format.Gray8, new byte[512 * 256]), null);
        Cache<ImageBufferCache.Key, DecodedImage> cache = ImageBufferCache.createCache(3L * first.imageBuffer().byteSize());
        EventQueue.invokeAndWait(() -> {
            cache.put(new TestKey("level", 0), first);
            cache.put(new TestKey("level", 1), second);
            cache.put(new TestKey("level", 2), third);
            // A frequently used image is still evicted once it is the least recently used.
            for (int repeat = 0; repeat < 100; repeat++)
                cache.getIfPresent(new TestKey("level", 0));
            cache.getIfPresent(new TestKey("level", 1));
            cache.getIfPresent(new TestKey("level", 2));
            first.retain(); // A layer may continue displaying an evicted image.
            cache.put(new TestKey("level", 3), large);
            if (cache.getIfPresent(new TestKey("level", 0)) != null || cache.getIfPresent(new TestKey("level", 1)) != null
                    || cache.getIfPresent(new TestKey("level", 2)) != third || cache.getIfPresent(new TestKey("level", 3)) != large)
                throw new AssertionError("Eviction did not follow recency and byte weight");
        });
        await(() -> !allocated(second), "LRU eviction did not free the unheld image");
        if (!allocated(first))
            throw new AssertionError("LRU eviction freed a layer-owned image");
        EventQueue.invokeAndWait(first::release);
        await(() -> !allocated(first), "Last layer release did not free the evicted image");
        cache.invalidateAll();
        await(() -> !allocated(third) && !allocated(large), "LRU cache removal did not free images");
    }

    private static void resolutionChange() throws Exception {
        // Scale the images and budget equally: 512 old frames fill the cache, as at 8 GiB with 4K grayscale.
        for (int side : new int[]{256, 128}) {
            Cache<ImageBufferCache.Key, DecodedImage> cache = ImageBufferCache.createCache(512L * 256 * 256);
            ArrayList<DecodedImage> images = new ArrayList<>();
            EventQueue.invokeAndWait(() -> {
                for (int frame = 0; frame < 512; frame++) {
                    DecodedImage image = image();
                    images.add(image);
                    cache.put(new TestKey("old", frame), image);
                }
                for (int loop = 0; loop < 20; loop++) {
                    for (int frame = 0; frame < 512; frame++)
                        cache.getIfPresent(new TestKey("old", frame));
                }
                for (int loop = 0; loop < 3; loop++) {
                    int misses = 0;
                    for (int frame = 0; frame < 512; frame++) {
                        TestKey key = new TestKey("new", frame);
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

}
