package org.helioviewer.jhv.view.j2k;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageBufferCache;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.view.BaseView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// The real view and reader thread on a JPIP movie, headless.
// Arguments: Kakadu library, bridge library, JPIP URI of a movie.
public final class J2KViewTest {

    private static final LinkedBlockingQueue<View.ImageData> images = new LinkedBlockingQueue<>();
    private static final LatestWorker<DecodedImage> worker = new LatestWorker<>("Test-Decoder");

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        System.load(arguments[0]);
        System.load(arguments[1]);
        URI uri = URI.create(arguments[2]);
        checkReaper(uri);
        Responses responses = capture(uri);
        checkDraining(responses);
        checkRetriesAndKeys(responses);

        Field timerField = Player.class.getDeclaredField("movieTimer");
        timerField.setAccessible(true);
        EDTTimer playback = (EDTTimer) timerField.get(null);

        // Paused, first shown at its coarsest level, which opening already made complete: the movie still downloads.
        J2KView view = open(uri);
        try {
            checkResolutionSelection(view);
            int frames = view.getMaximumFrameNumber() + 1;
            if (frames < 4)
                throw new AssertionError("Expected a movie with at least four frames");
            show(view, 0, Integer.MAX_VALUE);
            await(view, frames - 1);
            // A finer level of the shown frame is fetched and delivered without another request to decode.
            ResolutionSet.Level finer = view.getResolutionLevel(0, 3);
            show(view, 0, 3);
            View.ImageData image;
            do {
                image = images.poll(60, TimeUnit.SECONDS);
                if (image == null)
                    throw new AssertionError("Finer level was not delivered");
                boolean ready = image.imageBuffer().width == finer.width() && image.imageBuffer().height == finer.height();
                EventQueue.invokeAndWait(image.image()::close);
                if (ready)
                    break;
            } while (true);
            checkRefresh(view);
        } finally {
            close(view);
        }

        // Playing: the whole movie at one level, undisturbed and with the connection lost once.
        EventQueue.invokeAndWait(() -> {
            playback.setInitialDelay(Integer.MAX_VALUE); // playing, without advancing the timeline
            playback.start();
        });
        try {
            String[] reference = play(uri, false);
            if (!Arrays.equals(reference, play(uri, true)))
                throw new AssertionError("Movie pixels differ after a lost connection");
        } finally {
            EventQueue.invokeAndWait(playback::stop);
        }
        System.out.println("PASS: download start, latest refresh viewpoint and resolution, detached refresh, priority refresh, playing download and connection recovery");
        System.exit(0);
    }

    private record Abandoned(WeakReference<J2KView> view, J2KNative client, Object key, DecodedImage image) {}

    private static Abandoned abandon(URI uri) throws Exception {
        J2KView view = open(uri);
        J2KNative client = ((J2KSource) field(J2KView.class, view, "source")).client();
        Constructor<?> constructor = Class.forName(J2KView.class.getName() + "$DecodeKey")
                .getDeclaredConstructor(int.class, J2KParams.Decode.class, ImageFilter.Type.class);
        constructor.setAccessible(true);
        Object key = constructor.newInstance(field(J2KView.class, view, "serial"), new J2KParams.Decode(0, 0), ImageFilter.Type.None);
        DecodedImage image = new DecodedImage(ImageBuffer.fromBytes(8, 8, ImageBuffer.Format.Gray8, new byte[64]), null);
        ImageBufferCache.put(key, image);
        return new Abandoned(new WeakReference<>(view), client, key, image);
    }

    private static void checkReaper(URI uri) throws Exception {
        Abandoned abandoned = abandon(uri);
        EventQueue.invokeAndWait(() -> {}); // Release the last invocation event's reference to the view.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        for (;;) {
            System.gc();
            Thread.sleep(20);
            EventQueue.invokeAndWait(() -> {});
            if (abandoned.view.get() == null && ImageBufferCache.get(abandoned.key) == null) {
                try {
                    abandoned.client.frames();
                } catch (CancellationException closed) {
                    try {
                        abandoned.image.retain();
                        throw new AssertionError("Cleaner left the cached image allocated");
                    } catch (IllegalStateException released) {
                    }
                    break;
                }
            }
            if (System.nanoTime() > deadline)
                throw new AssertionError("Cleaner did not retire the abandoned view's cache and native source");
        }
        System.out.println("PASS: abandoned view's Cleaner retires cached pixels and closes its native source");
    }

    @SuppressWarnings("unchecked")
    private static void checkResolutionSelection(J2KView view) throws Exception {
        // Before signalling the reader, exercise the selector with square, tall, wide and unequal-scale geometry.
        J2KSource source = (J2KSource) field(J2KView.class, view, "source");
        AtomicReferenceArray<ResolutionSet> sets = (AtomicReferenceArray<ResolutionSet>) field(J2KSource.class, source, "sets");
        MetaData[] metadata = (MetaData[]) field(BaseView.class, view, "metaData");
        ResolutionSet originalSet = sets.get(0);
        MetaData originalMetadata = metadata[0];
        Method select = J2KView.class.getDeclaredMethod("getDecodeParams", int.class, double.class);
        select.setAccessible(true);
        try {
            for (int[] geometry : new int[][]{{4096, 4096, 4096, 4096, 4}, {2048, 4096, 2048, 4096, 4},
                    {4096, 2048, 4096, 2048, 4}, {4096, 4096, 8192, 4096, 3}}) {
                ResolutionSet.Level[] levels = new ResolutionSet.Level[6];
                for (int i = 0; i < levels.length; i++)
                    levels[i] = new ResolutionSet.Level(i, geometry[0] >> i, geometry[1] >> i);
                sets.set(0, new ResolutionSet(levels, 1));
                metadata[0] = new BasicMetaData(geometry[0], geometry[1], "Selection test") {
                    @Override
                    public Region getPhysicalRegion() {
                        return new Region(0, 0, geometry[2], geometry[3]);
                    }
                };
                J2KParams.Decode selected = (J2KParams.Decode) select.invoke(view, 0, 1.0 / 16);
                if (selected.level() != geometry[4])
                    throw new AssertionError("Wrong resolution for " + Arrays.toString(geometry) + ": " + selected.level());
            }
        } finally {
            sets.set(0, originalSet);
            metadata[0] = originalMetadata;
        }
        System.out.println("PASS: square, tall, wide and unequal-scale resolution selection");
    }

    private static void checkRefresh(J2KView view) throws Exception {
        ResolutionSet.Level size = view.getResolutionLevel(0, Integer.MAX_VALUE);
        MetaData metadata = view.getMetaData(view.getFrameTime(0));
        Position latest = Position.toFixedDistance(metadata.getViewpoint(), metadata.getViewpoint().distance);
        double scale = size.height() / metadata.getPhysicalRegion().height;
        clearImages();
        EventQueue.invokeAndWait(() -> view.decode(latest, scale, null));
        View.ImageData image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Latest request viewpoint was not delivered");
        EventQueue.invokeAndWait(image.image()::close);

        clearImages();
        // An older fine window finishes after the view has zoomed out.
        EventQueue.invokeAndWait(() -> view.refreshDecodeFromReader(0));
        image = images.poll(60, TimeUnit.SECONDS);
        if (image == null || image.viewpoint() != latest)
            throw new AssertionError("Reader refresh used an obsolete viewpoint");
        int width = image.imageBuffer().width;
        int height = image.imageBuffer().height;
        EventQueue.invokeAndWait(image.image()::close);
        if (width != size.width() || height != size.height())
            throw new AssertionError("Reader refresh used an obsolete resolution: " + width + "x" + height
                    + ", wanted " + size.width() + "x" + size.height());

        // The replacement view uses the same worker. An old view's cached refresh
        // must not invalidate its decode or drop its pending task.
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Boolean> fresh = new CompletableFuture<>();
        try {
            EventQueue.invokeAndWait(() -> {
                view.setDataHandler(null);
                worker.submit(() -> {
                    release.await();
                    return null;
                }, (result, current) -> fresh.complete(current));
                view.refreshDecodeFromReader(0);
            });
            EventQueue.invokeAndWait(() -> {}); // the queued refresh has run
            release.countDown();
            if (!fresh.get(10, TimeUnit.SECONDS))
                throw new AssertionError("Detached refresh invalidated the replacement decode");
        } finally {
            release.countDown();
            EventQueue.invokeAndWait(() -> view.setDataHandler(J2KViewTest::queueImage));
        }
    }

    private static String[] play(URI uri, boolean drop) throws Exception {
        J2KView view = open(uri);
        try {
            int frames = view.getMaximumFrameNumber() + 1;
            show(view, 0, 4);
            if (drop) {
                await(view, frames / 3);
                Field readerField = J2KView.class.getDeclaredField("reader");
                readerField.setAccessible(true);
                Object reader = readerField.get(view);
                Field socketField = J2KReader.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                ((JPIPSocket) socketField.get(reader)).abort();
            }
            long deadline = System.nanoTime() + 120_000_000_000L;
            while (!view.isComplete()) {
                if (System.nanoTime() > deadline)
                    throw new AssertionError("Movie did not complete");
                Thread.sleep(5);
            }

            String[] hashes = new String[frames];
            for (int frame = 0; frame < frames; frame++) {
                ResolutionSet.Level size = view.getResolutionLevel(frame, 4);
                show(view, frame, 4);
                View.ImageData image = images.poll(60, TimeUnit.SECONDS);
                if (image == null || image.imageBuffer().width != size.width() || image.imageBuffer().height != size.height())
                    throw new AssertionError("Complete frame " + frame + " was not delivered at its level");
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(((ByteBuffer) image.imageBuffer().buffer).duplicate());
                hashes[frame] = HexFormat.of().formatHex(hash.digest());
                EventQueue.invokeAndWait(image.image()::close);
            }
            return hashes;
        } finally {
            close(view);
        }
    }

    private static J2KView open(URI uri) throws Exception {
        return open(uri, null);
    }

    private static J2KView open(URI uri, APIRequest request) throws Exception {
        Constructor<DataUri> constructor = DataUri.class.getDeclaredConstructor(URI.class, URI.class, File.class);
        constructor.setAccessible(true);
        J2KView view = new J2KView(worker, request, constructor.newInstance(uri, uri, null), new ImageProcessingSettings(() -> {}));
        EventQueue.invokeAndWait(() -> view.setDataHandler(J2KViewTest::queueImage));
        return view;
    }

    private static void close(J2KView view) throws Exception {
        EventQueue.invokeAndWait(() -> {
            view.setDataHandler(null);
            view.abolish();
        });
        clearImages();
    }

    private static void queueImage(View.ImageData image) {
        image.image().retain();
        images.add(image);
    }

    private static void clearImages() throws Exception {
        EventQueue.invokeAndWait(() -> {
            View.ImageData image;
            while ((image = images.poll()) != null)
                image.image().close();
        });
    }

    // Asks for a frame at the level a display of that level's height would want.
    private static void show(J2KView view, int frame, int level) throws Exception {
        MetaData metadata = view.getMetaData(view.getFrameTime(frame));
        double scale = view.getResolutionLevel(frame, level).height() / metadata.getPhysicalRegion().height;
        AtomicBoolean selected = new AtomicBoolean();
        clearImages();
        EventQueue.invokeAndWait(() -> {
            selected.set(view.setNearestFrame(view.getFrameTime(frame)));
            if (selected.get())
                view.decode(metadata.getViewpoint(), scale, null);
        });
        if (!selected.get())
            throw new AssertionError("Frame " + frame + " cannot be selected");
    }

    private static void await(J2KView view, int frame) throws Exception {
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (view.getFrameCompletion(frame) == null) {
            if (System.nanoTime() > deadline)
                throw new AssertionError("Frame " + frame + " did not become displayable");
            Thread.sleep(1);
        }
    }

    private record Responses(List<byte[]> opening, int metadataEnd, List<byte[]> finer, byte[] secondFrame, int level, String[] dates) {}

    // Capture a normal opening and two small windows. Reader tests replay these locally,
    // so timing and failure counts do not depend on a live service or stored capture folders.
    private static Responses capture(URI uri) throws Exception {
        List<byte[]> opening = new ArrayList<>(), finer = new ArrayList<>();
        J2KSource source = new J2KSource(null);
        try {
            JPIPSocket socket = new JPIPSocket(uri);
            try {
                receive(socket, source, opening);
                int reason;
                do {
                    socket.sendMetadata();
                    reason = receive(socket, source, opening);
                } while (reason == 4 || reason == 7);
                int metadataEnd = opening.size();
                source.loadFrames();
                long stream = source.client().frame(0).stream();
                do {
                    socket.sendFrame(stream, 64, 64, 0);
                    reason = receive(socket, source, opening);
                } while (reason == 4 || reason == 7);
                source.update(0);
                ResolutionSet set = source.resolutionSet(0);
                if (!set.isDisplayable()) {
                    ResolutionSet.Level size = set.getClosestLevel(64, 64);
                    do {
                        socket.sendFrame(stream, size.width(), size.height(), 1);
                        reason = receive(socket, source, opening);
                    } while (reason == 4 || reason == 7);
                    source.update(0);
                }
                int level = source.resolutionSet(0).getCompleteLevel(0).level() - 1;
                if (level < 0 || source.getFrameStatus(0, level))
                    throw new AssertionError("Fixture needs an incomplete level finer than its opening");
                ResolutionSet.Level size = set.getLevel(level);
                do {
                    socket.sendFrame(stream, size.width(), size.height(), 1);
                    reason = receive(socket, source, finer);
                } while (reason == 4 || reason == 7);
                source.update(0);
                if (!source.getFrameStatus(0, level))
                    throw new AssertionError("Captured finer window is incomplete");
                String[] dates = new String[2];
                for (int i = 0; i < dates.length; i++) {
                    XMLMetaDataContainer xml = new XMLMetaDataContainer(source.xml(i));
                    dates[i] = xml.getString("DATE-AVG").or(() -> xml.getString("DATE_AVG"))
                            .or(() -> xml.getString("DATE_OBS")).orElseGet(() -> xml.getRequiredString("DATE-OBS"));
                }
                if (dates[0].length() != dates[1].length() || dates[0].equals(dates[1]))
                    throw new AssertionError("Fixture needs distinct timestamps of equal length");
                // Small windows let the drain check hold both responses after the two sends.
                socket.sendFrame(source.client().frame(1).stream(), size.width(), size.height(), 0);
                List<byte[]> secondFrame = new ArrayList<>();
                reason = receive(socket, source, secondFrame);
                source.update(1);
                if ((reason != 1 && reason != 2) || finer.size() != 1 || !Boolean.TRUE.equals(source.getFrameStatus(1, level)))
                    throw new AssertionError("Drain fixture needs two complete, single-response windows");
                return new Responses(opening, metadataEnd, finer, secondFrame.getFirst(), level, dates);
            } finally {
                socket.abort();
            }
        } finally {
            source.close();
        }
    }

    private static int receive(JPIPSocket socket, J2KSource source, List<byte[]> captured) throws Exception {
        ByteBuffer response = socket.receive();
        byte[] bytes = new byte[response.remaining()];
        response.duplicate().get(bytes);
        captured.add(bytes);
        int reason = source.client().response(response);
        if (reason != 1 && reason != 2 && reason != 4 && reason != 7)
            throw new AssertionError("Unexpected captured EOR: " + reason);
        return reason;
    }

    private static void checkDraining(Responses responses) throws Exception {
        CountDownLatch sent = new CountDownLatch(1), release = new CountDownLatch(1);
        J2KSource source = new J2KSource(null);
        J2KReader reader = null;
        try (ServerSocket listener = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
                ExecutorService tasks = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(10000);
            Future<?> served = tasks.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(10000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                    for (int i = 0; i < responses.opening.size(); i++) {
                        readRequest(input);
                        reply(connection, i == 0, responses.opening.get(i));
                    }
                    readRequest(input);
                    readRequest(input);
                    sent.countDown();
                    if (!release.await(5, TimeUnit.SECONDS))
                        throw new AssertionError("No newer signal after the two sends");
                    reply(connection, false, responses.finer.getFirst());
                    reply(connection, false, responses.secondFrame);
                    if (input.readLine() != null)
                        throw new AssertionError("Reader sent another request instead of draining");
                }
                return null;
            });
            try {
                reader = new J2KReader(URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/drain"), source);
                // Invoke only the prefetch pass; no worker consumes the real signal queue.
                Field keys = J2KReader.class.getDeclaredField("cacheKey");
                keys.setAccessible(true);
                keys.set(reader, new String[3]);
                J2KParams.Read params = new J2KParams.Read(null, new J2KParams.Decode(0, responses.level), false);
                Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, ResolutionSet.Level.class, boolean.class);
                readFrames.setAccessible(true);
                J2KReader current = reader;
                Future<Boolean> pump = tasks.submit(() -> (boolean) readFrames.invoke(current, params, source.resolutionSet(0).getLevel(responses.level), false));
                if (!sent.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Reader did not pipeline two requests");
                reader.signal(params);
                release.countDown();
                if (pump.get(5, TimeUnit.SECONDS) || !Boolean.TRUE.equals(source.getFrameStatus(0, responses.level))
                        || !Boolean.TRUE.equals(source.getFrameStatus(1, responses.level)) || source.geometry(2) != null)
                    throw new AssertionError("Reader did not drain both responses before yielding to newer work");
            } finally {
                release.countDown();
                if (reader != null)
                    reader.stop();
            }
            served.get(5, TimeUnit.SECONDS);
        } finally {
            source.close();
        }
        System.out.println("PASS: a real signal after two sends drains both responses without fetching the next frame");
    }

    private static Object field(Class<?> type, Object instance, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void checkRetriesAndKeys(Responses responses) throws Exception {
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
        System.out.println("PASS: immediate first retry, delayed consecutive retries, limit after partial responses, refusal, and per-frame disk keys");
    }

    private static void checkFaults(Responses responses, int failures, boolean refuse) throws Exception {
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
                        int openingEnd = session == 0 ? responses.opening.size() : responses.metadataEnd;
                        for (int i = 0; i < openingEnd; i++) {
                            readRequest(input);
                            reply(connection, i == 0, responses.opening.get(i));
                        }
                        readRequest(input);
                        requests.add(System.nanoTime());
                        byte[] prior = responses.opening.getLast().clone();
                        if (refuse) {
                            prior[prior.length - 4] ^= 1; // Conflict with an already held databin byte.
                            reply(connection, false, prior);
                        } else if (session < failures) {
                            if (!Arrays.equals(Arrays.copyOfRange(prior, prior.length - 3, prior.length), new byte[]{0, 2, 0}))
                                throw new AssertionError("Expected a window-done EOR at the end of the captured body");
                            prior[prior.length - 2] = 4; // Accepted byte-limited data, still unready at the requested level.
                            reply(connection, false, prior);
                            readRequest(input); // Then break this pass before it can complete a frame.
                            continue;
                        } else {
                            for (int i = 0; i < responses.finer.size(); i++) {
                                if (i > 0)
                                    readRequest(input);
                                reply(connection, false, responses.finer.get(i));
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
                            reply(connection, false, new byte[]{0, 2, 0});
                        }
                    }
                }
                return requests;
            });
            URI uri = URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test");
            J2KView view = open(uri, new APIRequest("ROB", 10, 0, 0, APIRequest.CADENCE_ALL));
            J2KReader reader = (J2KReader) field(J2KView.class, view, "reader");
            try {
                J2KSource source = (J2KSource) field(J2KView.class, view, "source");
                MetaData[] metadata = (MetaData[]) field(BaseView.class, view, "metaData");
                String[] keys = (String[]) field(J2KReader.class, reader, "cacheKey");
                for (int i = 0; i < keys.length; i++) {
                    String expected = metadata[i] instanceof FitsMetaData ? "10+" + metadata[i].getViewpoint().time.milli : null;
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
                reader.signal(new J2KParams.Read(view, new J2KParams.Decode(0, responses.level), true));
                if (refuse || failures == 14) {
                    if (!logged.await(30, TimeUnit.SECONDS))
                        throw new AssertionError("Reader never reported its terminal failure: " + records.stream().map(LogRecord::getMessage).toList());
                    if (refuse) {
                        Thread thread = (Thread) field(J2KReader.class, reader, "myThread");
                        thread.join(5000);
                        if (thread.isAlive() || source.getFrameStatus(0, responses.level))
                            throw new AssertionError("Refused reader continued or marked the finer level complete");
                        for (String key : keys) {
                            if (key != null && JPIPCacheManager.get(key, 0) != null)
                                throw new AssertionError("Refusal retained a disk entry for this source");
                        }
                        if (JPIPCacheManager.get("unrelated", 0) == null)
                            throw new AssertionError("Refusal removed another source's entry");
                    } else {
                        Thread thread = (Thread) field(J2KReader.class, reader, "myThread");
                        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                        while (thread.getState() != Thread.State.WAITING || view.isDownloading()) {
                            if (System.nanoTime() > deadline)
                                throw new AssertionError("Exhausted reader did not return to idle");
                            Thread.sleep(5);
                        }
                        if (!((JPIPSocket) field(J2KReader.class, reader, "socket")).isClosed())
                            throw new AssertionError("Exhausted reader opened another connection");
                    }
                } else {
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    while (!source.getFrameStatus(0, responses.level)) {
                        if (System.nanoTime() > deadline)
                            throw new AssertionError("Reader did not recover from two failures");
                        Thread.sleep(5);
                    }
                    Thread thread = (Thread) field(J2KReader.class, reader, "myThread");
                    while (thread.getState() != Thread.State.WAITING || view.isDownloading()) {
                        if (System.nanoTime() > deadline)
                            throw new AssertionError("Recovered reader did not return to idle");
                        Thread.sleep(5);
                    }
                }
                EventQueue.invokeAndWait(reader::stop); // Keep the headless EDT alive while interrupting queued refreshes.
                int retries = (int) field(J2KReader.class, reader, "retries");
                List<Long> requests = served.get(5, TimeUnit.SECONDS);
                if (!refuse) {
                    if (retries != (failures == 14 ? 14 : 0))
                        throw new AssertionError("Wrong consecutive failure count: " + retries);
                    double firstRetry = (requests.get(1) - requests.get(0)) / 1e6;
                    double secondRetry = (requests.get(2) - requests.get(1)) / 1e6;
                    if (firstRetry >= 900 || secondRetry < 900)
                        throw new AssertionError("Wrong retry pauses: " + firstRetry + ", " + secondRetry + " ms");
                    if (failures == 14 && (records.stream().filter(r -> r.getMessage().contains("Retry limit reached:")).count() != 1
                            || records.stream().noneMatch(r -> r.getMessage().contains("Retry limit reached:") && r.getThrown() != null)))
                        throw new AssertionError("Retry exhaustion did not log its exception once");
                }
            } finally {
                close(view);
                listener.close();
            }
        } finally {
            logger.removeHandler(capture);
            for (Handler handler : handlers)
                logger.addHandler(handler);
        }
    }

    private static void checkFrameOrder(Responses responses, boolean duplicate) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
                ExecutorService server = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(10000);
            Future<?> served = server.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(10000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                    for (int i = 0; i < responses.opening.size(); i++) {
                        readRequest(input);
                        // Equal-length substitutions preserve JPIP databin offsets and lengths.
                        String body = new String(responses.opening.get(i), StandardCharsets.ISO_8859_1);
                        if (duplicate) {
                            body = body.replace(responses.dates[1], responses.dates[0]);
                        } else {
                            String placeholder = "_".repeat(responses.dates[0].length());
                            body = body.replace(responses.dates[0], placeholder)
                                    .replace(responses.dates[1], responses.dates[0]).replace(placeholder, responses.dates[1]);
                        }
                        reply(connection, i == 0, body.getBytes(StandardCharsets.ISO_8859_1));
                    }
                    if (input.readLine() != null)
                        throw new AssertionError("Rejected movie left its connection open or started downloading");
                }
                return null;
            });
            URI uri = URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test");
            try {
                J2KView view = open(uri);
                close(view);
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

    private static void readRequest(BufferedReader input) throws Exception {
        String line = input.readLine();
        if (line == null || !line.startsWith("GET "))
            throw new AssertionError("Expected a JPIP request, got " + line);
        while ((line = input.readLine()) != null && !line.isEmpty()) {}
        if (line == null)
            throw new AssertionError("Truncated request headers");
    }

    private static void reply(Socket connection, boolean opening, byte[] body) throws Exception {
        OutputStream output = connection.getOutputStream();
        output.write(("HTTP/1.1 200 OK\r\nContent-Type: image/jpp-stream\r\n"
                + (opening ? "JPIP-cnew: cid=test,transport=http,path=jpip\r\n" : "")
                + "Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.write(body);
    }

    private J2KViewTest() {}
}
