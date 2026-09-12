package org.helioviewer.jhv.view.j2k;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.ArrayBlockingQueue;

import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.view.j2k.jpip.JPIPCacheManager;
import org.helioviewer.jhv.view.j2k.jpip.JPIPSocket;

// Exercise the production prefetch pump directly, without constructing a GUI view.
public final class MovieReaderTest {
    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        KakaduMessageSystem.startKduMessageSystem();
        URI uri = URI.create(arguments[1]);
        Method close = JPIPCacheManager.class.getDeclaredMethod("close");
        close.setAccessible(true);
        JPIPCacheManager.init();
        try {
            String[] reference = retrieve(uri, false, false);
            String[] pipelined = retrieve(uri, true, false);
            if (!Arrays.equals(reference, pipelined))
                throw new AssertionError("Movie pixels differ with overlapping requests");
            close.invoke(null);
            JPIPCacheManager.init();
            if (!Arrays.equals(reference, retrieve(uri, true, true)))
                throw new AssertionError("Movie pixels differ after cached restoration");
            System.out.println("PASS: four movie frames have identical pixels with sequential requests, reader prefetch and reopened cache");
        } finally {
            close.invoke(null);
        }
    }

    private static String[] retrieve(URI uri, boolean pump, boolean cached) throws Exception {
        J2KSource.Remote source = new J2KSource.Remote();
        J2KReader reader = null;
        JPIPSocket socket = null;
        try {
            if (pump) {
                reader = new J2KReader(uri, source);
                reader.setCacheKey(new String[]{"movie0", "movie1", "movie2", "movie3"});
            } else {
                socket = new JPIPSocket(uri, source.cache());
                socket.init(source.cache());
            }
            source.open();
            if (source.maxFrame() < 3)
                throw new AssertionError("Expected a movie with at least four frames");
            if (pump) {
                if (cached) {
                    Field socketField = J2KReader.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    ((JPIPSocket) socketField.get(reader)).abort(); // The pump must satisfy every frame from cache.
                }
                Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, String.class, boolean.class);
                readFrames.setAccessible(true);
                ResolutionSet.Level size = source.resolutionSet(0).getLevel(0);
                J2KParams.Read params = new J2KParams.Read(null, source,
                        new J2KParams.Decode(0, size.subImage(), 0, 1), null, false);
                if (!cached) {
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
                    if ((boolean) readFrames.invoke(reader, params, "4096,4096", false))
                        throw new AssertionError("Pump ignored newer work");
                    if (!source.getFrameStatus(0, 0).get() || !source.getFrameStatus(1, 0).get()
                            || source.getFrameStatus(2, 0) != null)
                        throw new AssertionError("Pump did not stop after draining the two sent responses");
                    signals.clear();
                }
                if (!(boolean) readFrames.invoke(reader, params, "4096,4096", false))
                    throw new AssertionError("Prefetch interrupted unexpectedly");
            } else {
                for (int frame = 0; frame < 4; frame++) {
                    do {
                        socket.sendFrame(frame, "4096,4096");
                    } while (!socket.receiveFrame(source.cache()).complete());
                    source.setFrameComplete(frame, 0);
                }
            }
            MetaData metadata = (MetaData) Proxy.newProxyInstance(MovieReaderTest.class.getClassLoader(),
                    new Class<?>[]{MetaData.class}, (proxy, method, values) -> {
                        if (method.getName().equals("roiToRegion"))
                            return Region.DEFAULT;
                        throw new AssertionError("Unexpected metadata call: " + method.getName());
                    });
            String[] hashes = new String[4];
            for (int frame = 0; frame < hashes.length; frame++) {
                if (!source.getFrameStatus(frame, 0).get())
                    throw new AssertionError("Incomplete frame " + frame);
                ResolutionSet resolution = source.resolutionSet(frame);
                ResolutionSet.Level size = resolution.getLevel(0);
                DecodedImage image = new J2KDecoder(source,
                        new J2KParams.Decode(frame, size.subImage(), 0, 1), resolution.numComps,
                        ImageFilter.Type.None, metadata, size.factorX(), size.factorY()).call();
                if (image == null)
                    throw new AssertionError("Missing decoded frame " + frame);
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(((ByteBuffer) image.imageBuffer().buffer).duplicate());
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
                source.destroy();
            }
        }
    }

    private MovieReaderTest() {}
}
