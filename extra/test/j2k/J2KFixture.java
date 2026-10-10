package org.helioviewer.jhv.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.source.jpip.JPIPSocket;

// Reader-level fixtures for the live decoder test, in the package of the reader internals they use.
public final class J2KFixture {

    public record Responses(List<byte[]> opening, int metadataEnd, List<byte[]> finer, byte[] secondFrame, int level, String[] dates) {}

    // Capture a normal opening and two small windows. Reader tests replay these locally,
    // so timing and failure counts do not depend on a live service or stored capture folders.
    public static Responses capture(URI uri) throws Exception {
        List<byte[]> opening = new ArrayList<>(), finer = new ArrayList<>();
        J2KSource source = new J2KSource(null);
        try {
            JPIPSocket socket = new JPIPSocket(uri);
            try {
                receive(socket, source, opening, -1);
                J2KReader.readWindow(socket::sendMetadata, () -> receive(socket, source, opening, -1));
                int metadataEnd = opening.size();
                source.loadFrames();
                long stream = source.client().frame(0).stream();
                J2KReader.readWindow(() -> socket.sendFrame(stream, 64, 64, 0),
                        () -> receive(socket, source, opening, 0));
                source.update(0);
                ResolutionSet set = source.levels(0);
                if (!set.isDisplayable()) {
                    ResolutionSet.Level size = set.getClosestLevel(64, 64);
                    J2KReader.readWindow(() -> socket.sendFrame(stream, size.width(), size.height(), 1),
                            () -> receive(socket, source, opening, 0));
                    source.update(0);
                }
                int level = source.levels(0).getCompleteLevel(0).level() - 1;
                if (level < 0 || source.getFrameStatus(0, level))
                    throw new AssertionError("Fixture needs an incomplete level finer than its opening");
                ResolutionSet.Level size = set.getLevel(level);
                J2KReader.readWindow(() -> socket.sendFrame(stream, size.width(), size.height(), 1),
                        () -> receive(socket, source, finer, 0));
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
                int reason = receive(socket, source, secondFrame, 1).reason();
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

    private static J2KNative.Response receive(JPIPSocket socket, J2KSource source, List<byte[]> captured, int window) throws IOException {
        ByteBuffer response = socket.receive();
        byte[] bytes = new byte[response.remaining()];
        response.duplicate().get(bytes);
        captured.add(bytes);
        J2KNative.Response parsed = source.client().response(response, window);
        int reason = parsed.reason();
        if (reason != 1 && reason != 2 && reason != 4 && reason != 7)
            throw new AssertionError("Unexpected captured EOR: " + reason);
        return parsed;
    }

    public static void checkDraining(Responses responses) throws Exception {
        CountDownLatch sent = new CountDownLatch(1), release = new CountDownLatch(1), decoding = new CountDownLatch(1);
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
                J2KParams.Read params = new J2KParams.Read(new J2KParams.Decode(0, responses.level), false);
                Method readFrames = J2KReader.class.getDeclaredMethod("readFrames", J2KParams.Read.class, ResolutionSet.Level.class, boolean.class);
                readFrames.setAccessible(true);
                J2KReader current = reader;
                Future<Boolean> pump = tasks.submit(() -> (boolean) readFrames.invoke(current, params, source.levels(0).getLevel(responses.level), false));
                if (!sent.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Reader did not pipeline two requests");
                ResolutionSet.Level coarse = source.level(0, Integer.MAX_VALUE);
                byte[] expected = source.decodeRegion(0, coarse.level(), 0, 0, coarse.width(), coarse.height());
                Future<?> pixels = tasks.submit(() -> {
                    decoding.countDown();
                    do {
                        if (!Arrays.equals(expected, source.decodeRegion(0, coarse.level(), 0, 0, coarse.width(), coarse.height())))
                            throw new AssertionError("Complete-level pixels changed during ingestion");
                    } while (!pump.isDone());
                    return null;
                });
                if (!decoding.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Concurrent decode did not start");
                reader.signal(params);
                release.countDown();
                if (pump.get(5, TimeUnit.SECONDS) || !Boolean.TRUE.equals(source.getFrameStatus(0, responses.level))
                        || !Boolean.TRUE.equals(source.getFrameStatus(1, responses.level)) || source.geometry(2) != null)
                    throw new AssertionError("Reader did not drain both responses before yielding to newer work");
                pixels.get(5, TimeUnit.SECONDS);
                if (!Arrays.equals(expected, source.decodeRegion(0, coarse.level(), 0, 0, coarse.width(), coarse.height())))
                    throw new AssertionError("Complete-level pixels changed after ingestion");
            } finally {
                release.countDown();
                if (reader != null)
                    reader.stop();
            }
            served.get(5, TimeUnit.SECONDS);
        } finally {
            source.close();
        }
        System.out.println("PASS: a real signal drains both responses, and complete-level pixels stay unchanged during ingestion");
    }

    public static Boolean frameStatus(J2KSource source, int frame, int level) {
        return source.getFrameStatus(frame, level);
    }

    public static void readRequest(BufferedReader input) throws Exception {
        String line = input.readLine();
        if (line == null || !line.startsWith("GET "))
            throw new AssertionError("Expected a JPIP request, got " + line);
        while ((line = input.readLine()) != null && !line.isEmpty()) {}
        if (line == null)
            throw new AssertionError("Truncated request headers");
    }

    public static void reply(Socket connection, boolean opening, byte[] body) throws Exception {
        OutputStream output = connection.getOutputStream();
        output.write(("HTTP/1.1 200 OK\r\nContent-Type: image/jpp-stream\r\n"
                + (opening ? "JPIP-cnew: cid=test,transport=http,path=jpip\r\n" : "")
                + "Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.write(body);
    }

    private J2KFixture() {}
}
