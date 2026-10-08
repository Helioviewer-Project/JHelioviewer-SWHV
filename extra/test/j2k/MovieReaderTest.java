package org.helioviewer.jhv.view.j2k;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.ArrayBlockingQueue;

import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// Exercise the production prefetch pump directly, without constructing a GUI view.
// Arguments: Kakadu library, bridge library, JPIP URI of a movie with at least four frames.
public final class MovieReaderTest {

    private enum Mode { SEQUENTIAL, PUMP, CACHED, DAMAGED }

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        System.load(arguments[1]);
        URI uri = URI.create(arguments[2]);
        Method close = JPIPCacheManager.class.getDeclaredMethod("close");
        close.setAccessible(true);
        JPIPCacheManager.init();
        try {
            String[] reference = retrieve(uri, Mode.SEQUENTIAL);
            if (!Arrays.equals(reference, retrieve(uri, Mode.PUMP)))
                throw new AssertionError("Movie pixels differ with overlapping requests");
            close.invoke(null);
            JPIPCacheManager.init();
            if (!Arrays.equals(reference, retrieve(uri, Mode.CACHED)))
                throw new AssertionError("Movie pixels differ after cached restoration");
            if (!Arrays.equals(reference, retrieve(uri, Mode.DAMAGED)))
                throw new AssertionError("Movie pixels differ after a damaged cache entry");
            System.out.println("PASS: four movie frames have identical pixels with sequential requests, reader prefetch, reopened cache and a damaged cache entry");
        } finally {
            close.invoke(null);
        }
    }

    private static boolean isComplete(int reason) {
        if (reason != 1 && reason != 2 && reason != 4 && reason != 7)
            throw new AssertionError("Unexpected end of response: " + reason);
        return reason < 4;
    }

    private static String[] retrieve(URI uri, Mode mode) throws Exception {
        J2KSource source = new J2KSource(null);
        J2KReader reader = null;
        JPIPSocket socket = null;
        try {
            if (mode == Mode.SEQUENTIAL) { // the requests of the reader, one at a time, without it
                J2KNative client = source.client();
                socket = new JPIPSocket(uri);
                client.response(socket.receive());
                do {
                    socket.sendMetadata();
                } while (!isComplete(client.response(socket.receive())));
                source.loadFrames();
                for (int frame = 0; frame < 4; frame++) {
                    do {
                        socket.sendFrame(client.frame(frame).stream(), "4096,4096", "4097,4097");
                    } while (!isComplete(client.response(socket.receive())));
                    source.update(frame);
                }
            } else {
                reader = new J2KReader(uri, source);
                reader.setCacheKey(new String[]{"movie0", "movie1", "movie2", "movie3"});
            }
            if (source.frames() < 4)
                throw new AssertionError("Expected a movie with at least four frames");
            ResolutionSet.Level size = source.resolutionSet(0).getLevel(0);
            if (size.width() != 4096 || size.height() != 4096)
                throw new AssertionError("Expected 4096x4096 frames");

            if (reader != null) {
                if (mode == Mode.CACHED) {
                    Field socketField = J2KReader.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    ((JPIPSocket) socketField.get(reader)).abort(); // The pump must satisfy every frame from cache.
                }
                if (mode == Mode.DAMAGED) { // a block the client refuses: the frame is fetched and stored again
                    JPIPCacheManager.remove("movie2");
                    JPIPCacheManager.store("movie2", 0, () -> new byte[]{1, 2, 3});
                }
                Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, ResolutionSet.Level.class, boolean.class);
                readFrames.setAccessible(true);
                Field retries = J2KReader.class.getDeclaredField("retries");
                retries.setAccessible(true);
                retries.setInt(reader, 12);
                J2KParams.Read params = new J2KParams.Read(null, new J2KParams.Decode(0, 0), false);
                if (mode == Mode.PUMP) {
                    // Keep the idle worker waiting on its original queue while invoking the pump directly.
                    Field threadField = J2KReader.class.getDeclaredField("myThread");
                    threadField.setAccessible(true);
                    Thread worker = (Thread) threadField.get(reader);
                    long deadline = System.nanoTime() + 5_000_000_000L;
                    while (worker.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
                        Thread.sleep(1);
                    if (worker.getState() != Thread.State.WAITING)
                        throw new AssertionError("Reader did not become idle");
                    ArrayBlockingQueue<J2KParams.Read> signals = new ArrayBlockingQueue<>(1) {
                        private int checks;

                        @Override
                        public boolean isEmpty() {
                            // Newer work arrives after two sends; both responses must still be drained.
                            if (++checks == 3)
                                offer(params);
                            return super.isEmpty();
                        }
                    };
                    Field queueField = J2KReader.class.getDeclaredField("signalQueue");
                    queueField.setAccessible(true);
                    queueField.set(reader, signals);
                    if ((boolean) readFrames.invoke(reader, params, size, false))
                        throw new AssertionError("Pump ignored newer work");
                    if (source.geometry(0) == null || source.geometry(1) == null || source.geometry(2) != null)
                        throw new AssertionError("Pump did not stop after draining the two sent responses");
                    signals.clear();
                }
                if (!(boolean) readFrames.invoke(reader, params, size, false))
                    throw new AssertionError("Prefetch interrupted unexpectedly");
                if (retries.getInt(reader) != 0)
                    throw new AssertionError("Received or restored frames did not reset consecutive failures");
                JPIPCacheManager.Entry entry = JPIPCacheManager.get("movie2", 0);
                if (entry == null || entry.block().length < 1000)
                    throw new AssertionError("Missing or damaged cache entry after the pump");
            }

            String[] hashes = new String[4];
            for (int frame = 0; frame < hashes.length; frame++) {
                if (!source.getFrameStatus(frame, 0))
                    throw new AssertionError("Incomplete frame " + frame);
                ByteBuffer pixels;
                try (J2KNative.Decode job = source.beginDecode(frame, 0)) {
                    pixels = ByteBuffer.allocateDirect(size.width() * size.height());
                    J2KSource.decode(job, 0, 0, size.width(), size.height(), pixels);
                }
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(pixels);
                hashes[frame] = HexFormat.of().formatHex(hash.digest());
            }
            return hashes;
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

    private MovieReaderTest() {}
}
