package org.helioviewer.jhv.view.j2k;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// Raw reader pixels, independent of the view's delivery and filtering path.
// Arguments: Kakadu library, bridge library, single-frame JPIP URI, movie JPIP URI.
public final class J2KReaderTest {

    private enum Mode { SEQUENTIAL, PREFETCH, CACHED, DAMAGED }

    public static void main(String[] arguments) throws Exception {
        testResolutions();
        testContinuation();
        if (arguments.length == 0) {
            System.out.println("PASS: resolution boundaries, reader continuation, stalled windows, EOR validation and interruption without native libraries");
            return;
        }
        System.load(arguments[0]);
        System.load(arguments[1]);
        Method close = JPIPCacheManager.class.getDeclaredMethod("close");
        close.setAccessible(true);
        JPIPCacheManager.init();
        try {
            URI image = URI.create(arguments[2]), movie = URI.create(arguments[3]);
            String[] normal = retrieve(image, 1, new int[]{2, 0}, Mode.PREFETCH);
            close.invoke(null);
            JPIPCacheManager.init();
            compare(normal, retrieve(image, 1, new int[]{2, 0}, Mode.CACHED), "single-frame cache restoration");

            String[] reference = retrieve(movie, 4, new int[]{0}, Mode.SEQUENTIAL);
            compare(reference, retrieve(movie, 4, new int[]{0}, Mode.PREFETCH), "movie prefetch");
            close.invoke(null);
            JPIPCacheManager.init();
            compare(reference, retrieve(movie, 4, new int[]{0}, Mode.CACHED), "movie cache restoration");
            compare(reference, retrieve(movie, 4, new int[]{0}, Mode.DAMAGED), "damaged movie cache entry");
            System.out.println("PASS: progressive single-frame retrieval, movie prefetch, persisted and damaged caches preserve metadata and pixels");
        } finally {
            close.invoke(null);
        }
    }

    private static void testResolutions() {
        ResolutionSet set = new ResolutionSet(new ResolutionSet.Level[]{
                new ResolutionSet.Level(0, 100, 60), new ResolutionSet.Level(1, 50, 30),
                new ResolutionSet.Level(2, 25, 15)}, 1);
        for (int[] expected : new int[][]{{101, 61, 0}, {100, 60, 0}, {51, 30, 0}, {50, 31, 0},
                {50, 30, 1}, {49, 29, 1}, {26, 15, 1}, {25, 16, 1}, {25, 15, 2}, {24, 14, 2}, {0, 0, 2}}) {
            if (set.getNextLevel(expected[0], expected[1]).level() != expected[2])
                throw new AssertionError("Wrong resolution at boundary " + Arrays.toString(expected));
        }
        ResolutionSet single = new ResolutionSet(new ResolutionSet.Level[]{new ResolutionSet.Level(0, 100, 60)}, 1);
        if (single.getNextLevel(1, 1).level() != 0 || single.getNextLevel(101, 61).level() != 0)
            throw new AssertionError("Single resolution selection");
        if (set.isDisplayable())
            throw new AssertionError("Unready resolutions are displayable");
        set.setCompleteLevels(1);
        if (!set.getComplete(2) || set.getComplete(1) || set.getCompleteLevel(0).level() != 2)
            throw new AssertionError("Coarsest completion");
        set.setCompleteLevels(2);
        if (!set.getComplete(1) || set.getComplete(0) || set.getCompleteLevel(0).level() != 1)
            throw new AssertionError("Progressive completion");
    }

    private static void testContinuation() throws Exception {
        int[] calls = {0};
        J2KReader.readWindow(() -> calls[0]++, () -> new J2KNative.Response(calls[0] < 20 ? 4 : 2, true));
        if (calls[0] != 20)
            throw new AssertionError("Legitimate continuation was cut short");
        for (int reason : new int[]{4, 7}) {
            calls[0] = 0;
            try {
                J2KReader.readWindow(() -> {
                    if (++calls[0] > 2)
                        throw new AssertionError("Stalled window kept requesting");
                }, () -> new J2KNative.Response(reason, calls[0] == 1));
                throw new AssertionError("Stalled window kept requesting");
            } catch (IOException expected) {
                if (calls[0] != 2)
                    throw new AssertionError("Stalled continuation was retried");
            }
            if (J2KReader.window(new J2KNative.Response(reason, false)) != J2KReader.Window.STALLED)
                throw new AssertionError("Frame response did not report stalled delivery");
        }
        J2KReader.readWindow(() -> {}, () -> new J2KNative.Response(2, false));
        try {
            J2KReader.window(new J2KNative.Response(3, true));
            throw new AssertionError("Unexpected EOR was accepted");
        } catch (IOException expected) {}
        calls[0] = 0;
        Thread.currentThread().interrupt();
        try {
            J2KReader.readWindow(() -> calls[0]++, () -> new J2KNative.Response(4, true));
            throw new AssertionError("Interrupted window kept requesting");
        } catch (InterruptedIOException expected) {
            if (calls[0] != 0)
                throw new AssertionError("Interrupted window sent a request");
        } finally {
            Thread.interrupted();
        }
    }

    private static void compare(String[] expected, String[] actual, String scenario) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError("Metadata or pixels differ after " + scenario);
    }

    private static String[] retrieve(URI uri, int frames, int[] levels, Mode mode) throws Exception {
        J2KSource source = new J2KSource(null);
        J2KReader reader = null;
        JPIPSocket socket = null;
        try {
            Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, ResolutionSet.Level.class, boolean.class);
            readFrames.setAccessible(true);
            Field retries = J2KReader.class.getDeclaredField("retries");
            retries.setAccessible(true);
            String[] keys = new String[frames];
            for (int frame = 0; frame < frames; frame++)
                keys[frame] = uri + "[" + frame + "]";
            if (mode == Mode.SEQUENTIAL) {
                socket = new JPIPSocket(uri);
                source.client().newChannel();
                source.client().response(socket.receive(), -1);
                JPIPSocket opened = socket;
                J2KReader.readWindow(opened::sendMetadata, () -> source.client().response(opened.receive(), -1));
                source.loadFrames();
            } else {
                reader = new J2KReader(uri, source);
                reader.start(keys); // The worker stays idle; this test invokes the prefetch pass directly.
                if (mode == Mode.CACHED) {
                    Field socketField = J2KReader.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    ((JPIPSocket) socketField.get(reader)).abort(); // Only opening may use the network.
                }
                if (mode == Mode.DAMAGED) {
                    JPIPCacheManager.remove(keys[2]);
                    JPIPCacheManager.store(keys[2], 0, () -> new byte[]{1, 2, 3});
                }
            }
            if (source.frames() < frames || (frames == 1 && source.frames() != 1))
                throw new AssertionError("Wrong number of fixture frames");
            String[] result = new String[frames * (levels.length + 1)];
            for (int frame = 0; frame < frames; frame++) {
                String xml = source.xml(frame);
                if (xml == null || xml.isBlank())
                    throw new AssertionError("Missing metadata for frame " + frame);
                result[frame] = xml;
            }
            if (reader != null)
                retries.setInt(reader, 12);
            int index = frames;
            for (int level : levels) {
                ResolutionSet.Level size = reader == null ? new ResolutionSet.Level(0, 4096, 4096) : source.resolutionSet(0).getLevel(level);
                if (frames == 1 && mode == Mode.PREFETCH
                        && (Boolean.TRUE.equals(source.getFrameStatus(0, level)) || JPIPCacheManager.get(keys[0], level) != null))
                    throw new AssertionError("Level " + level + " complete or cached before retrieval");
                if (reader == null) {
                    for (int frame = 0; frame < frames; frame++) {
                        JPIPSocket opened = socket;
                        long stream = source.client().frame(frame).stream();
                        int window = frame;
                        J2KReader.readWindow(() -> opened.sendFrame(stream, size.width(), size.height(), 1),
                                () -> source.client().response(opened.receive(), window));
                        source.update(frame);
                    }
                } else {
                    J2KParams.Read params = new J2KParams.Read(null, new J2KParams.Decode(0, level), false);
                    if (!(boolean) readFrames.invoke(reader, params, size, false))
                        throw new AssertionError("Prefetch interrupted unexpectedly");
                    if (retries.getInt(reader) != 0)
                        throw new AssertionError("Completed or restored frames did not reset consecutive failures");
                }
                if (source.getPartialUntil() < frames - 1)
                    throw new AssertionError("Displayable prefix did not advance through retrieved frames");
                if (frames == 1 && (!source.isComplete(level)
                        || (mode == Mode.PREFETCH && level > 0 && source.isComplete(level - 1))))
                    throw new AssertionError("Source completion does not match the retrieved level");
                for (int frame = 0; frame < frames; frame++) {
                    if (!source.resolutionSet(frame).getLevel(level).equals(size))
                        throw new AssertionError("Fixture frames have different resolution grids");
                    if (!Boolean.TRUE.equals(source.getFrameStatus(frame, level)))
                        throw new AssertionError("Incomplete frame " + frame + " at level " + level);
                    if (reader != null) {
                        JPIPCacheManager.Entry entry = JPIPCacheManager.get(keys[frame], level);
                        if (entry == null || entry.level() != (mode == Mode.CACHED ? 0 : level) || entry.block().length < 1000)
                            throw new AssertionError("Missing or damaged persisted frame " + frame + " at level " + level);
                    }
                    if (frames == 1 && mode == Mode.PREFETCH && level > 0 && Boolean.TRUE.equals(source.getFrameStatus(frame, level - 1)))
                        throw new AssertionError("Fetching level " + level + " completed a finer level");
                    try (J2KNative.Decode job = source.client().beginDecode(frame, level)) {
                        ByteBuffer pixels = ByteBuffer.allocateDirect(size.width() * size.height());
                        J2KSource.decode(job, 0, 0, size.width(), size.height(), pixels);
                        MessageDigest hash = MessageDigest.getInstance("SHA-256");
                        hash.update(pixels);
                        result[index++] = HexFormat.of().formatHex(hash.digest());
                    }
                }
            }
            return result;
        } finally {
            try {
                if (reader != null)
                    reader.stop();
                if (socket != null)
                    socket.abort();
            } finally {
                source.close();
            }
        }
    }

    private J2KReaderTest() {}
}
