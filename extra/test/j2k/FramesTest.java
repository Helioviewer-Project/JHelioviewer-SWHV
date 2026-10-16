package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageGrid;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageGeometry;

import org.lwjgl.system.MemoryUtil;

// Timeline selection, synthesis and decoder delivery with an in-memory source.
public final class FramesTest {

    // A source whose frames become displayable as the test says.
    private static class Stub implements Source {
        final boolean[] ready;

        Stub(int count) {
            ready = new boolean[count];
        }

        @Override public int frames() { return ready.length; }
        @Override public String xml(int frame) { return null; }
        @Override public ResolutionSet levels(int frame) { throw new UnsupportedOperationException(); }
        @Override public LUT lut() { return null; }
        @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) throws Exception {
            throw new UnsupportedOperationException();
        }
        @Override public boolean displayable(int frame) { return ready[frame]; }
        @Override public void close() {}
    }

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice(); // synthetic frames carry an Earth viewpoint
        selection();
        synthesis();
        delivery(false);
        delivery(true);
        sampling();
        arrivals();
        System.out.println("PASS: nearest selectable frame by time, equal synthetic endpoints give one frame");
    }

    private static void selection() {
        long base = 1_800_000_000L; // 2027, inside the bundled ephemerides
        long[] seconds = {base, base + 100, base + 101, base + 102};
        Stub source = new Stub(seconds.length);
        List<Frames.Frame> list = new ArrayList<>();
        for (int i = 0; i < seconds.length; i++) {
            JHVTime time = new JHVTime(seconds[i] * 1000);
            list.add(new Frames.Frame(time, null, Frames.EMPTY_METAXML, source, i)); // metadata unused
        }
        Frames frames = new Frames(list);
        try {
            source.ready[0] = true;
            source.ready[3] = true;
            if (!frames.select(new JHVTime((base + 100) * 1000)) || frames.current() != 3)
                throw new AssertionError("Requested 100 s with 0 s and 102 s available selected frame " + frames.current());
            if (frames.select(new JHVTime((base + 100) * 1000 + 500)) || frames.current() != 3)
                throw new AssertionError("Unchanged nearest selectable frame reported a move");
            source.ready[3] = false;
            if (!frames.select(new JHVTime((base + 101) * 1000)) || frames.current() != 0)
                throw new AssertionError("Only frame 0 available but selected " + frames.current());
            source.ready[0] = false;
            if (frames.select(new JHVTime((base + 50) * 1000)) || frames.current() != 0)
                throw new AssertionError("Nothing available must keep the selection");
            source.ready[1] = true;
            if (!frames.select(frames.requested()) || frames.current() != 1)
                throw new AssertionError("A frame becoming available was not selected on retry");
            long t = base * 1000;
            if (frames.nearest(new JHVTime(t + 101_400)).milli != t + 101_000 || frames.lower(new JHVTime(t + 101_000)).milli != t + 100_000
                    || frames.higher(new JHVTime(t + 101_000)).milli != t + 102_000 || frames.higher(new JHVTime(t + 102_000)).milli != t + 102_000)
                throw new AssertionError("Time navigation over all frames is wrong");
        } finally {
            frames.close();
        }
    }

    private static void synthesis() {
        long t = 1_800_000_000_000L;
        Frames same = Frames.synthetic(t, t, 60);
        if (same.size() != 1 || same.first().milli != t)
            throw new AssertionError("Equal endpoints did not give one frame");
        same.close();
        Frames span = Frames.synthetic(t, t + 150_000, 60);
        if (span.size() != 4 || span.last().milli != t + 150_000 || span.time(1).milli != t + 60_000)
            throw new AssertionError("Synthetic span has the wrong frames: " + span.size());
        span.close();
    }

    private static void delivery(boolean dispose) throws Exception {
        ResolutionSet levels = completeLevels();
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1), delivered = new CountDownLatch(1);
        AtomicReference<ImageBuffer> pixels = new AtomicReference<>();
        AtomicInteger decodes = new AtomicInteger();
        Stub source = new Stub(1) {
            @Override public ResolutionSet levels(int frame) { return levels; }
            @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) throws Exception {
                ImageBuffer image = ImageBuffer.fromBytes(2, 2, new byte[]{1, 2, 3, 4});
                pixels.set(image);
                if (decodes.incrementAndGet() == 1) {
                    started.countDown();
                    // A native decode may return pixels even after disposal interrupts its worker.
                    for (;;) {
                        try {
                            if (!finish.await(5, TimeUnit.SECONDS))
                                throw new AssertionError("Test did not release the blocked decode");
                            break;
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
                return image;
            }
        };
        source.ready[0] = true;
        BasicMetaData metadata = new BasicMetaData(2, 2, "Delivery test");
        Frames frames = new Frames(List.of(new Frames.Frame(new JHVTime(1_800_000_000_000L), metadata, Frames.EMPTY_METAXML, source, 0)));
        ImageProcessingSettings settings = new ImageProcessingSettings(() -> {});
        int[] deliveries = {0}; // EDT only
        FrameDecoder decoder = new FrameDecoder(settings, data -> {
            if (!allocated(data.imageBuffer()))
                throw new AssertionError("Delivery borrowed freed pixels");
            deliveries[0]++;
            delivered.countDown();
        });
        try {
            EventQueue.invokeAndWait(() -> {
                decoder.reset(frames.serial());
                decoder.decode(frames, metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
            });
            if (!started.await(5, TimeUnit.SECONDS) || !allocated(pixels.get()))
                throw new AssertionError("Decode did not allocate its pixels; enable native allocator tracking");
            ImageBuffer stale = pixels.get();
            EventQueue.invokeAndWait(() -> {
                if (dispose)
                    decoder.dispose();
                else
                    decoder.reset(-1);
            });
            finish.countDown();
            awaitReleased(stale);
            EventQueue.invokeAndWait(() -> {
                if (deliveries[0] != 0)
                    throw new AssertionError("Cancelled decoder delivered its running decode");
            });
            if (dispose) {
                System.out.println("PASS: disposal during decode releases the returned pixels without delivery");
                return;
            }
            EventQueue.invokeAndWait(() -> {
                decoder.reset(frames.serial());
                decoder.decode(frames, metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
            });
            if (!delivered.await(5, TimeUnit.SECONDS))
                throw new AssertionError("Current timeline did not deliver its decode");
            // Each cache hit queues delivery. Change ownership/settings in the same EDT turn, before it runs.
            EventQueue.invokeAndWait(() -> {
                decoder.decode(frames, metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
                decoder.reset(-1);
            });
            EventQueue.invokeAndWait(() -> {
                decoder.reset(frames.serial());
                decoder.decode(frames, metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
                settings.setFilter(ImageFilter.Type.MGN);
            });
            EventQueue.invokeAndWait(() -> {
                settings.setFilter(ImageFilter.Type.None);
                decoder.decode(frames, metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
                decoder.dispose();
            });
            EventQueue.invokeAndWait(() -> {
                if (deliveries[0] != 1 || decodes.get() != 2 || !allocated(pixels.get()))
                    throw new AssertionError("Stale cached delivery escaped, decoded again or lost the cache's pixels");
            });
        } finally {
            finish.countDown();
            EventQueue.invokeAndWait(() -> {
                decoder.dispose();
                frames.close();
            });
        }
        awaitReleased(pixels.get());
        System.out.println("PASS: decoder releases replaced results, skips stale queued deliveries and frees cached pixels on close");
    }

    private static void arrivals() throws Exception {
        ResolutionSet levels = completeLevels();
        AtomicInteger requests = new AtomicInteger();
        Stub source = new Stub(3) {
            @Override public ResolutionSet levels(int frame) { return levels; }
            @Override public Boolean completion(int frame) { return ready[frame] ? Boolean.TRUE : null; }
            @Override public void request(int frame, int level, boolean priority) {
                if (priority)
                    throw new AssertionError("Arrival changed playback's request priority");
                requests.incrementAndGet();
            }
            @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) {
                return ImageBuffer.fromBytes(2, 2, new byte[]{(byte) frame, 2, 3, 4});
            }
        };
        source.ready[0] = true;
        BasicMetaData metadata = new BasicMetaData(2, 2, "Arrival test");
        List<Frames.Frame> list = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            list.add(new Frames.Frame(new JHVTime(1_800_000_000_000L + i * 60_000L), metadata, Frames.EMPTY_METAXML, source, i));
        Frames frames = new Frames(list);
        CountDownLatch[] shown = {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};
        ImageLayer layer = new ImageLayer(frames) {
            @Override public void handleData(ImageData data) {
                shown[data.frame().index()].countDown();
            }
        };
        int[] notifiedSelection = {-1};
        try {
            EventQueue.invokeAndWait(() -> {
                Layers.addListener(new Layers.Listener() {
                    @Override public void completionUpdated(ImageLayer changed) {
                        if (changed == layer)
                            notifiedSelection[0] = changed.frames().current();
                    }
                });
                layer.setVisible(0);
                layer.decode(metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
            });
            if (!shown[0].await(5, TimeUnit.SECONDS))
                throw new AssertionError("Initial frame was not delivered");
            EventQueue.invokeAndWait(() -> {
                frames.select(frames.time(1));
                source.ready[1] = true;
                layer.frameReady(frames, source, 1);
                if (notifiedSelection[0] != 1)
                    throw new AssertionError("Completion listeners saw the selection before catch-up");
            });
            if (!shown[1].await(5, TimeUnit.SECONDS))
                throw new AssertionError("Visible layer did not show the requested frame when it arrived");
            EventQueue.invokeAndWait(() -> {
                layer.setVisible(-1);
                frames.select(frames.time(2));
                CompletableFuture<Boolean> finished = layer.whenLoadFinished();
                if (finished.isDone())
                    throw new AssertionError("Load finished before the last frame arrived");
                source.ready[2] = true;
                layer.frameReady(frames, source, 2);
                if (requests.get() != 2 || frames.current() != 2 || notifiedSelection[0] != 2 || !finished.isDone() || !finished.join())
                    throw new AssertionError("Hidden arrival decoded, failed to select or left its load unfinished");
                layer.setVisible(0);
                layer.decode(metadata.getViewpoint(), Double.POSITIVE_INFINITY, false);
            });
            if (!shown[2].await(5, TimeUnit.SECONDS))
                throw new AssertionError("Re-shown layer did not deliver its current frame");
        } finally {
            EventQueue.invokeAndWait(layer::detach);
        }
        System.out.println("PASS: arrivals publish catch-up before notification; hidden layers settle without decoding and resume when shown");
    }

    private static void sampling() throws Exception {
        ResolutionSet levels = completeLevels(new ImageGrid(0, 128, 96), new ImageGrid(1, 64, 48),
                new ImageGrid(2, 32, 24), new ImageGrid(3, 16, 12));
        ImageGeometry originalGeometry = new ImageGeometry(128, 96, 1);
        BasicMetaData metadata = new BasicMetaData(128, 96, "Sampling test") {
            @Override public ImageGeometry getGeometry() { return originalGeometry; }
        };
        byte[] samples = new byte[16 * 12];
        for (int i = 0; i < samples.length; i++)
            samples[i] = (byte) (1 + i * 37 % 254);
        AtomicInteger decodes = new AtomicInteger();
        Stub source = new Stub(1) {
            @Override public ResolutionSet levels(int frame) { return levels; }
            @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) {
                if (level != 3)
                    throw new AssertionError("Sampling test did not select the reduced level");
                decodes.incrementAndGet();
                return ImageBuffer.fromBytes(16, 12, samples, filter);
            }
        };
        source.ready[0] = true;
        Frames frames = new Frames(List.of(new Frames.Frame(new JHVTime(1_800_000_000_000L), metadata, Frames.EMPTY_METAXML, source, 0)));
        // Independent expected texel-cell bounds: centers are (-63.5 + 8*x, -47.5 + 8*y).
        DecodedImage expected = new DecodedImage(ImageBuffer.fromBytes(16, 12, samples,
                ImageFilter.of(ImageFilter.Type.RHEF, new ImageGeometry(16, 12, originalGeometry.wcs,
                        new Mat2(8, 0, 0, -8), new Vec2(67.5 / 8, 51.5 / 8)))), originalGeometry);
        ImageProcessingSettings settings = new ImageProcessingSettings(() -> {});
        settings.setFilter(ImageFilter.Type.RHEF);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicInteger deliveries = new AtomicInteger();
        FrameDecoder decoder = new FrameDecoder(settings, data -> {
            try {
                if (data.image().geometry().original != originalGeometry)
                    throw new AssertionError("Reduced decode changed the original bounds used by cutouts");
                if (!((ShortBuffer) data.imageBuffer().buffer).equals((ShortBuffer) expected.imageBuffer().buffer))
                    throw new AssertionError("RHEF did not use the decoded sample positions");
                ImageGeometry geometry = data.image().geometry();
                if (geometry.sunPixel.x != 67.5 / 8 || geometry.sunPixel.y != 51.5 / 8)
                    throw new AssertionError("Decoded geometry lost the Sun's pixel position");
                deliveries.incrementAndGet();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                delivered.countDown();
            }
        });
        try {
            EventQueue.invokeAndWait(() -> {
                decoder.reset(frames.serial());
                decoder.decode(frames, metadata.getViewpoint(), 0, false);
            });
            if (!delivered.await(5, TimeUnit.SECONDS))
                throw new AssertionError("Sampling test did not deliver the decode", failure.get());
            EventQueue.invokeAndWait(() -> decoder.decode(frames, metadata.getViewpoint(), 0, false));
            EventQueue.invokeAndWait(() -> {}); // Deliver the queued cache hit.
            if (failure.get() != null)
                throw new AssertionError("Sampling geometry", failure.get());
            if (decodes.get() != 1 || deliveries.get() != 2)
                throw new AssertionError("Cached sampling test decoded again or did not deliver");
        } finally {
            EventQueue.invokeAndWait(() -> { decoder.dispose(); frames.close(); });
            expected.release();
        }
        System.out.println("PASS: reduced RHEF uses sample positions, original cutout bounds survive and cached playback does not decode again");
    }

    private static ResolutionSet completeLevels(ImageGrid... resolutions) throws Exception {
        Constructor<ResolutionSet> constructor = ResolutionSet.class.getDeclaredConstructor(ImageGrid[].class, int.class);
        constructor.setAccessible(true);
        if (resolutions.length == 0)
            resolutions = new ImageGrid[]{new ImageGrid(0, 2, 2)};
        ResolutionSet levels = constructor.newInstance(resolutions, 1);
        Method complete = ResolutionSet.class.getDeclaredMethod("setCompleteLevels", int.class);
        complete.setAccessible(true);
        complete.invoke(levels, resolutions.length);
        return levels;
    }

    private static boolean allocated(ImageBuffer image) {
        long address = MemoryUtil.memAddress(image.buffer);
        AtomicBoolean found = new AtomicBoolean();
        MemoryUtil.memReport((allocation, bytes, thread, name, stack) -> {
            if (allocation == address)
                found.set(true);
        });
        return found.get();
    }

    private static void awaitReleased(ImageBuffer image) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (allocated(image)) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("Decoder left native pixels allocated");
            EventQueue.invokeAndWait(() -> {});
            Thread.sleep(5);
        }
    }

    private FramesTest() {}
}
