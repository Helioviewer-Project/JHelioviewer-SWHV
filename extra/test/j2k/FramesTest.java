package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.time.JHVTime;

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
        @Override public ClipSet clipSet() { return null; }
        @Override public boolean usesFITSParameters() { return false; }
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
        delivery();
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

    private static void delivery() throws Exception {
        Constructor<ResolutionSet> constructor = ResolutionSet.class.getDeclaredConstructor(ResolutionSet.Level[].class, int.class);
        constructor.setAccessible(true);
        ResolutionSet levels = constructor.newInstance(new ResolutionSet.Level[]{new ResolutionSet.Level(0, 2, 2)}, 1);
        Method complete = ResolutionSet.class.getDeclaredMethod("setCompleteLevels", int.class);
        complete.setAccessible(true);
        complete.invoke(levels, 1);
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1), delivered = new CountDownLatch(1);
        AtomicReference<ImageBuffer> pixels = new AtomicReference<>();
        AtomicInteger decodes = new AtomicInteger();
        Stub source = new Stub(1) {
            @Override public ResolutionSet levels(int frame) { return levels; }
            @Override public ImageBuffer decode(int frame, int level, ImageFilter filter, ImageProcessingSettings.FITSParameters fits, ClipSet.Range clip) throws Exception {
                ImageBuffer image = ImageBuffer.fromBytes(2, 2, ImageBuffer.Format.Gray8, new byte[]{1, 2, 3, 4});
                pixels.set(image);
                if (decodes.incrementAndGet() == 1) {
                    started.countDown();
                    if (!finish.await(5, TimeUnit.SECONDS))
                        throw new AssertionError("Test did not release the blocked decode");
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
            EventQueue.invokeAndWait(() -> decoder.reset(-1));
            finish.countDown();
            awaitReleased(stale);
            EventQueue.invokeAndWait(() -> {
                if (deliveries[0] != 0)
                    throw new AssertionError("Replaced timeline delivered its running decode");
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
