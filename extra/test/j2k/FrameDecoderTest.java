package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.source.J2KFixture;
import org.helioviewer.jhv.source.J2KSource;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.jpip.JPIPCacheManager;
import org.helioviewer.jhv.source.jpip.JPIPSocket;

// The real timeline, decoder and reader thread on a JPIP movie, headless.
// Arguments: Kakadu library, bridge library, JPIP URI of a movie.
public final class FrameDecoderTest {

    private static final LinkedBlockingQueue<ImageData> images = new LinkedBlockingQueue<>();
    private static final Class<?> readerClass;

    static {
        try {
            readerClass = Class.forName("org.helioviewer.jhv.source.J2KReader");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] arguments) throws Exception {
        long timeout = Long.getLong("jhv.test.timeoutSeconds", 480L);
        CompletableFuture.delayedExecutor(timeout, TimeUnit.SECONDS).execute(() -> {
            System.err.println("FrameDecoderTest exceeded its " + timeout + " second budget");
            System.exit(1);
        });
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        System.load(arguments[0]);
        System.load(arguments[1]);
        URI uri = URI.create(arguments[2]);
        if (Boolean.getBoolean("jhv.test.cleaner"))
            checkReaper(uri);
        J2KFixture.Responses responses = J2KFixture.capture(uri);
        J2KFixture.checkDraining(responses);
        checkRetriesAndKeys(responses);

        // Paused, first shown at its coarsest level, which opening already made complete: the movie still downloads.
        Opened opened = open(uri);
        try {
            checkResolutionSelection(opened);
            int frames = opened.frames.size();
            if (frames < 4)
                throw new AssertionError("Expected a movie with at least four frames");
            show(opened, 0, Integer.MAX_VALUE, true);
            await(opened, frames - 1);
            // A finer level of the shown frame is fetched and delivered without another request to decode.
            ResolutionSet.Level finer = opened.source.level(0, 3);
            show(opened, 0, 3, true);
            ImageData image;
            do {
                image = images.poll(60, TimeUnit.SECONDS);
                if (image == null)
                    throw new AssertionError("Finer level was not delivered");
                boolean ready = image.imageBuffer().width == finer.width() && image.imageBuffer().height == finer.height();
                EventQueue.invokeAndWait(image.image()::release);
                if (ready)
                    break;
            } while (true);
            checkRefresh(opened);
        } finally {
            close(opened);
        }

        // Playing, so without priority: the whole movie at one level, undisturbed and with the connection lost once.
        String[] reference = play(uri, false);
        if (!Arrays.equals(reference, play(uri, true)))
            throw new AssertionError("Movie pixels differ after a lost connection");
        System.out.println("PASS: download start, latest refresh viewpoint and resolution, replaced-timeline refresh, priority refresh, playing download and connection recovery");
        System.exit(0);
    }

    private record Opened(Frames frames, FrameDecoder decoder, J2KSource source) {}

    private record Abandoned(WeakReference<Frames> frames, Object client, ImageBufferCache.Key key, DecodedImage image) {}

    private static Abandoned abandon(URI uri) throws Exception {
        Opened opened = open(uri);
        Method client = J2KSource.class.getDeclaredMethod("client");
        client.setAccessible(true);
        Constructor<?> constructor = Class.forName(FrameDecoder.class.getName() + "$Key")
                .getDeclaredConstructor(int.class, int.class, int.class, ImageFilter.Type.class,
                        ImageProcessingSettings.FITSParameters.class, ClipSet.Range.class);
        constructor.setAccessible(true);
        ImageBufferCache.Key key = (ImageBufferCache.Key) constructor.newInstance(opened.frames.serial(), 0, 0, ImageFilter.Type.None, null, null);
        DecodedImage image = new DecodedImage(ImageBuffer.fromBytes(8, 8, ImageBuffer.Format.Gray8, new byte[64]), null);
        ImageBufferCache.put(key, image);
        EventQueue.invokeAndWait(opened.decoder::dispose);
        return new Abandoned(new WeakReference<>(opened.frames), client.invoke(opened.source), key, image);
    }

    private static void checkReaper(URI uri) throws Exception {
        Abandoned abandoned = abandon(uri);
        EventQueue.invokeAndWait(() -> {}); // Release the last invocation event's reference to the timeline.
        Method frames = abandoned.client.getClass().getDeclaredMethod("frames");
        frames.setAccessible(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        for (;;) {
            System.gc();
            Thread.sleep(20);
            EventQueue.invokeAndWait(() -> {});
            if (abandoned.frames.get() == null && ImageBufferCache.get(abandoned.key) == null) {
                try {
                    frames.invoke(abandoned.client);
                } catch (InvocationTargetException closed) {
                    if (!(closed.getCause() instanceof CancellationException))
                        throw closed;
                    try {
                        abandoned.image.retain();
                        throw new AssertionError("Cleaner left the cached image allocated");
                    } catch (IllegalStateException released) {
                    }
                    break;
                }
            }
            if (System.nanoTime() > deadline)
                throw new AssertionError("Cleaner did not retire the abandoned timeline's cache and native source");
        }
        System.out.println("PASS: abandoned timeline's Cleaner retires cached pixels and closes its native source");
    }

    @SuppressWarnings("unchecked")
    private static void checkResolutionSelection(Opened opened) throws Exception {
        // Before signalling the reader, exercise the selector with square, tall, wide and unequal-scale geometry.
        AtomicReferenceArray<ResolutionSet> sets = (AtomicReferenceArray<ResolutionSet>) field(J2KSource.class, opened.source, "sets");
        ResolutionSet originalSet = sets.get(0);
        Constructor<ResolutionSet> constructor = ResolutionSet.class.getDeclaredConstructor(ResolutionSet.Level[].class, int.class);
        constructor.setAccessible(true);
        try {
            for (int[] geometry : new int[][]{{4096, 4096, 4096, 4096, 4}, {2048, 4096, 2048, 4096, 4},
                    {4096, 2048, 4096, 2048, 4}, {4096, 4096, 8192, 4096, 3}}) {
                ResolutionSet.Level[] levels = new ResolutionSet.Level[6];
                for (int i = 0; i < levels.length; i++)
                    levels[i] = new ResolutionSet.Level(i, geometry[0] >> i, geometry[1] >> i);
                sets.set(0, constructor.newInstance(levels, 1));
                MetaData metadata = new BasicMetaData(geometry[0], geometry[1], "Selection test") {
                    @Override
                    public Region getPhysicalRegion() {
                        return new Region(0, 0, geometry[2], geometry[3]);
                    }
                };
                Frames.Frame frame = new Frames.Frame(opened.frames.time(0), metadata, Frames.EMPTY_METAXML, opened.source, 0);
                int selected = FrameDecoder.level(frame, 1.0 / 16);
                if (selected != geometry[4])
                    throw new AssertionError("Wrong resolution for " + Arrays.toString(geometry) + ": " + selected);
            }
        } finally {
            sets.set(0, originalSet);
        }
        System.out.println("PASS: square, tall, wide and unequal-scale resolution selection");
    }

    private static void checkRefresh(Opened opened) throws Exception {
        Frames frames = opened.frames;
        FrameDecoder decoder = opened.decoder;
        ResolutionSet.Level size = opened.source.level(0, Integer.MAX_VALUE);
        MetaData metadata = frames.get(0).metaData();
        Position latest = Position.toFixedDistance(metadata.getViewpoint(), metadata.getViewpoint().distance);
        double scale = size.height() / metadata.getPhysicalRegion().height;
        clearImages();
        EventQueue.invokeAndWait(() -> decoder.decode(frames, latest, scale, true));
        ImageData image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Latest request viewpoint was not delivered");
        EventQueue.invokeAndWait(image.image()::release);

        clearImages();
        // An older fine window finishes after the display has zoomed out.
        frames.frameUpdated(opened.source, 0);
        image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Reader refresh used an obsolete viewpoint");
        int width = image.imageBuffer().width;
        int height = image.imageBuffer().height;
        EventQueue.invokeAndWait(image.image()::release);
        if (width != size.width() || height != size.height())
            throw new AssertionError("Reader refresh used an obsolete resolution: " + width + "x" + height
                    + ", wanted " + size.width() + "x" + size.height());

        // A refresh of a replaced timeline must not deliver.
        clearImages();
        EventQueue.invokeAndWait(() -> decoder.reset(-1));
        frames.frameUpdated(opened.source, 0);
        if (images.poll(2, TimeUnit.SECONDS) != null)
            throw new AssertionError("Refresh after replacement delivered an image");
    }

    private static String[] play(URI uri, boolean drop) throws Exception {
        Opened opened = open(uri);
        try {
            int frames = opened.frames.size();
            show(opened, 0, 4, false);
            if (drop) {
                await(opened, frames / 3);
                Object reader = field(J2KSource.class, opened.source, "reader");
                ((JPIPSocket) field(readerClass, reader, "socket")).abort();
            }
            long deadline = System.nanoTime() + 120_000_000_000L;
            while (!opened.frames.isComplete()) {
                if (System.nanoTime() > deadline)
                    throw new AssertionError("Movie did not complete");
                Thread.sleep(5);
            }

            String[] hashes = new String[frames];
            for (int frame = 0; frame < frames; frame++) {
                ResolutionSet.Level size = opened.source.level(frame, 4);
                show(opened, frame, 4, false);
                ImageData image = images.poll(60, TimeUnit.SECONDS);
                if (image == null || image.imageBuffer().width != size.width() || image.imageBuffer().height != size.height())
                    throw new AssertionError("Complete frame " + frame + " was not delivered at its level");
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(((ByteBuffer) image.imageBuffer().buffer).duplicate());
                hashes[frame] = HexFormat.of().formatHex(hash.digest());
                EventQueue.invokeAndWait(image.image()::release);
            }
            return hashes;
        } finally {
            close(opened);
        }
    }

    private static Opened open(URI uri) throws Exception {
        return open(uri, null);
    }

    // The timeline and a decoder wired as the layer wires them.
    private static Opened open(URI uri, APIRequest request) throws Exception {
        Frames frames = ImageLayerLoader.open(request, List.of(uri)).frames();
        FrameDecoder decoder = new FrameDecoder(new ImageProcessingSettings(() -> {}), FrameDecoderTest::queueImage);
        EventQueue.invokeAndWait(() -> {
            decoder.reset(frames.serial());
            frames.setListener((f, source, frame) -> {
                boolean moved = f.select(f.requested());
                Frames.Frame current = f.get(f.current());
                if (moved)
                    decoder.redecode(f, true);
                else if (current.source() == source && current.index() == frame)
                    decoder.redecode(f, false);
            });
        });
        return new Opened(frames, decoder, (J2KSource) frames.get(0).source());
    }

    private static void close(Opened opened) throws Exception {
        EventQueue.invokeAndWait(() -> {
            opened.decoder.dispose();
            opened.frames.setListener(null);
            opened.frames.close();
        });
        clearImages();
    }

    private static void queueImage(ImageData image) {
        image.image().retain();
        images.add(image);
    }

    private static void clearImages() throws Exception {
        EventQueue.invokeAndWait(() -> {
            ImageData image;
            while ((image = images.poll()) != null)
                image.image().release();
        });
    }

    // Asks for a frame at the level a display of that level's height would want.
    private static void show(Opened opened, int frame, int level, boolean priority) throws Exception {
        Frames frames = opened.frames;
        MetaData metadata = frames.get(frame).metaData();
        double scale = opened.source.level(frame, level).height() / metadata.getPhysicalRegion().height;
        clearImages();
        EventQueue.invokeAndWait(() -> {
            frames.select(frames.time(frame));
            if (frames.current() == frame)
                opened.decoder.decode(frames, metadata.getViewpoint(), scale, priority);
        });
        if (frames.current() != frame)
            throw new AssertionError("Frame " + frame + " cannot be selected");
    }

    private static void await(Opened opened, int frame) throws Exception {
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (opened.frames.completion(frame) == null) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("Frame " + frame + " did not become displayable");
            Thread.sleep(1);
        }
    }

    private static Object field(Class<?> type, Object instance, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void stopReader(Object reader) throws Exception {
        Method stop = readerClass.getDeclaredMethod("stop");
        stop.setAccessible(true);
        stop.invoke(reader);
    }

    private static void checkRetriesAndKeys(J2KFixture.Responses responses) throws Exception {
        checkFrameOrder(responses, false);
        checkFrameOrder(responses, true);
        JPIPCacheManager.init();
        try {
            checkFaults(responses, 2, false);
            checkFaults(responses, 14, false);
            checkFaults(responses, 0, true);
        } finally {
            Method close = JPIPCacheManager.class.getDeclaredMethod("close");
            close.setAccessible(true);
            close.invoke(null);
        }
        System.out.println("PASS: delayed consecutive retries, limit after partial responses, refusal, and per-frame disk keys");
    }

    private static void checkFaults(J2KFixture.Responses responses, int failures, boolean refuse) throws Exception {
        CountDownLatch logged = new CountDownLatch(1);
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Logger logger = Logger.getLogger("");
        Handler[] handlers = logger.getHandlers();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) {
                records.add(record);
                if (record.getMessage().contains("Retry limit reached:") || record.getMessage().contains("conflicts"))
                    logged.countDown();
            }
            @Override public void flush() {}
            @Override public void close() {}
        };
        for (Handler handler : handlers)
            logger.removeHandler(handler);
        logger.addHandler(capture);
        try (ServerSocket listener = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
                ExecutorService server = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(10000);
            Future<List<Long>> served = server.submit(() -> {
                List<Long> requests = new ArrayList<>();
                int sessions = failures == 14 ? 14 : failures + 1;
                for (int session = 0; session < sessions; session++) {
                    try (Socket connection = listener.accept()) {
                        connection.setSoTimeout(10000);
                        connection.setTcpNoDelay(true);
                        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                        int openingEnd = session == 0 ? responses.opening().size() : responses.metadataEnd();
                        for (int i = 0; i < openingEnd; i++) {
                            J2KFixture.readRequest(input);
                            J2KFixture.reply(connection, i == 0, responses.opening().get(i));
                        }
                        J2KFixture.readRequest(input);
                        requests.add(System.nanoTime());
                        byte[] prior = responses.opening().getLast().clone();
                        if (refuse) {
                            prior[prior.length - 4] ^= 1; // Conflict with an already held databin byte.
                            J2KFixture.reply(connection, false, prior);
                        } else if (session < failures) {
                            if (!Arrays.equals(Arrays.copyOfRange(prior, prior.length - 3, prior.length), new byte[]{0, 2, 0}))
                                throw new AssertionError("Expected a window-done EOR at the end of the captured body");
                            prior[prior.length - 2] = 4; // Accepted byte-limited data, still unready at the requested level.
                            J2KFixture.reply(connection, false, prior);
                            J2KFixture.readRequest(input); // Then break this pass before it can complete a frame.
                            continue;
                        } else {
                            for (int i = 0; i < responses.finer().size(); i++) {
                                if (i > 0)
                                    J2KFixture.readRequest(input);
                                J2KFixture.reply(connection, false, responses.finer().get(i));
                            }
                        }
                        // Settle the remaining movie frames with empty completed windows,
                        // so successful recovery reaches idle before the test closes it.
                        String line;
                        while ((line = input.readLine()) != null) {
                            if (!line.startsWith("GET "))
                                throw new AssertionError("Unexpected request: " + line);
                            while ((line = input.readLine()) != null && !line.isEmpty()) {}
                            if (line == null)
                                throw new AssertionError("Truncated request headers");
                            J2KFixture.reply(connection, false, new byte[]{0, 2, 0});
                        }
                    }
                }
                return requests;
            });
            URI uri = URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test");
            Opened opened = open(uri, new APIRequest("ROB", 10, 0, 0, APIRequest.CADENCE_ALL));
            Object reader = field(J2KSource.class, opened.source, "reader");
            try {
                Frames frames = opened.frames;
                J2KSource source = opened.source;
                String[] keys = (String[]) field(readerClass, reader, "cacheKey");
                if (keys.length != frames.size())
                    throw new AssertionError("Disk keys do not cover the timeline");
                for (int i = 0; i < keys.length; i++) {
                    MetaData metadata = frames.get(i).metaData();
                    String expected = metadata instanceof FitsMetaData ? "10+" + metadata.getViewpoint().time.milli : null;
                    if (!Objects.equals(keys[i], expected))
                        throw new AssertionError("Disk key uses a sorted position or a fallback time at frame " + i);
                    if (keys[i] != null)
                        JPIPCacheManager.remove(keys[i]); // Each fault case must download, not restore the preceding case.
                }
                if (refuse) {
                    for (String key : keys) {
                        if (key != null)
                            JPIPCacheManager.store(key, 0, () -> new byte[]{1});
                    }
                    JPIPCacheManager.store("unrelated", 0, () -> new byte[]{2});
                }
                EventQueue.invokeAndWait(() -> source.request(0, responses.level(), true));
                if (refuse || failures == 14) {
                    if (!logged.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Reader never reported its terminal failure: " + records.stream().map(LogRecord::getMessage).toList());
                    if (refuse) {
                        Thread thread = (Thread) field(readerClass, reader, "myThread");
                        thread.join(5000);
                        if (thread.isAlive() || J2KFixture.frameStatus(source, 0, responses.level()))
                            throw new AssertionError("Refused reader continued or marked the finer level complete");
                        for (String key : keys) {
                            if (key != null && JPIPCacheManager.get(key, 0) != null)
                                throw new AssertionError("Refusal retained a disk entry for this source");
                        }
                        if (JPIPCacheManager.get("unrelated", 0) == null)
                            throw new AssertionError("Refusal removed another source's entry");
                    } else {
                        Thread thread = (Thread) field(readerClass, reader, "myThread");
                        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                        while (thread.getState() != Thread.State.WAITING || frames.isDownloading()) {
                            if (System.nanoTime() > deadline)
                                throw new AssertionError("Exhausted reader did not return to idle");
                            Thread.sleep(5);
                        }
                        if (!((JPIPSocket) field(readerClass, reader, "socket")).isClosed())
                            throw new AssertionError("Exhausted reader opened another connection");
                    }
                } else {
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (!J2KFixture.frameStatus(source, 0, responses.level())) {
                        if (System.nanoTime() > deadline)
                            throw new AssertionError("Reader did not recover from two failures");
                        Thread.sleep(5);
                    }
                    Thread thread = (Thread) field(readerClass, reader, "myThread");
                    while (thread.getState() != Thread.State.WAITING || frames.isDownloading()) {
                        if (System.nanoTime() > deadline)
                            throw new AssertionError("Recovered reader did not return to idle");
                        Thread.sleep(5);
                    }
                }
                EventQueue.invokeAndWait(() -> { // Keep the headless EDT alive while interrupting queued refreshes.
                    try {
                        stopReader(reader);
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                });
                int retries = (int) field(readerClass, reader, "retries");
                List<Long> requests = served.get(5, TimeUnit.SECONDS);
                if (!refuse) {
                    if (retries != (failures == 14 ? 14 : 0))
                        throw new AssertionError("Wrong consecutive failure count: " + retries);
                    double secondRetry = (requests.get(2) - requests.get(1)) / 1e6;
                    if (secondRetry < 900)
                        throw new AssertionError("Second retry did not pause: " + secondRetry + " ms");
                    if (failures == 14 && (records.stream().filter(r -> r.getMessage().contains("Retry limit reached:")).count() != 1
                            || records.stream().noneMatch(r -> r.getMessage().contains("Retry limit reached:") && r.getThrown() != null)))
                        throw new AssertionError("Retry exhaustion did not log its exception once");
                }
            } finally {
                close(opened);
                listener.close();
            }
        } finally {
            logger.removeHandler(capture);
            for (Handler handler : handlers)
                logger.addHandler(handler);
        }
    }

    private static void checkFrameOrder(J2KFixture.Responses responses, boolean duplicate) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
                ExecutorService server = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(10000);
            Future<?> served = server.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(10000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                    for (int i = 0; i < responses.opening().size(); i++) {
                        J2KFixture.readRequest(input);
                        // Equal-length substitutions preserve JPIP databin offsets and lengths.
                        String body = new String(responses.opening().get(i), StandardCharsets.ISO_8859_1);
                        if (duplicate) {
                            body = body.replace(responses.dates()[1], responses.dates()[0]);
                        } else {
                            String placeholder = "_".repeat(responses.dates()[0].length());
                            body = body.replace(responses.dates()[0], placeholder)
                                    .replace(responses.dates()[1], responses.dates()[0]).replace(placeholder, responses.dates()[1]);
                        }
                        J2KFixture.reply(connection, i == 0, body.getBytes(StandardCharsets.ISO_8859_1));
                    }
                    if (input.readLine() != null)
                        throw new AssertionError("Rejected movie left its connection open or started downloading");
                }
                return null;
            });
            URI uri = URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test");
            try {
                Opened opened = open(uri);
                close(opened);
                throw new AssertionError("Accepted " + (duplicate ? "duplicate" : "out-of-order") + " timestamps");
            } catch (Exception e) {
                String expected = duplicate ? "Duplicate frame timestamp" : "Out-of-order frame timestamp";
                if (!e.getMessage().contains(expected) || !e.getMessage().contains("Frame 0:")
                        || !e.getMessage().contains("Frame 1:") || !e.getMessage().contains(uri.toString()))
                    throw new AssertionError("Incomplete ordering diagnostic", e);
            }
            served.get(5, TimeUnit.SECONDS);
        }
        System.out.println("PASS: " + (duplicate ? "duplicate" : "out-of-order") + " movie rejected and connection closed");
    }

    private FrameDecoderTest() {}
}
