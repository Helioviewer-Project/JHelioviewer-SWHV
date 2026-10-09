package org.helioviewer.jhv.source.jpip;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public final class JPIPSocketTest {

    private static final String CHANNEL = "JPIP-cnew: cid=test,transport=http,path=jpip\r\n";

    public static void main(String[] arguments) throws Exception {
        testResponse("content-length: 3\r\n\r\n", true);
        testResponse("Content-Length: -1\r\n\r\n", false);
        testResponse("Content-Length: 3\r\n", false);
        for (String cnew : new String[]{"cid=test,transport=http", "transport=http,path=jpip",
                "cid=test,path=jpip", "cid=test,transport=http-tcp,path=jpip"})
            testInvalidChannel("JPIP-cnew: " + cnew + "\r\n");
        testInvalidChannel("");
        for (String encoding : new String[]{"identity", "gzip", "deflate"})
            testPipeline(encoding);
        testInterruptedHandshake();
        testClose(false);
        testClose(true);
        System.out.println("PASS: channel opening, plain/gzip/deflate chunked bodies in request order, buffer growth, graceful close, abort and cancelled handshake");
    }

    private interface Server {
        Object serve(Socket connection, BufferedReader input) throws Exception;
    }

    private interface Client {
        void run(URI uri, ExecutorService workers, Future<Object> served) throws Exception;
    }

    // Runs the client code against one connection of a local server; returns what the server returned.
    private static Object exchange(Server server, Client client) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            Future<Object> served = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    return server.serve(connection, new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII)));
                }
            });
            client.run(URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), workers, served);
            return served.get(5, TimeUnit.SECONDS);
        }
    }

    private static void testResponse(String framing, boolean valid) throws Exception {
        exchange((connection, input) -> {
            String request = readRequest(input);
            if (!request.startsWith("GET /test?cnew=http&type=jpp-stream&tid=0&len=512 "))
                throw new IOException("Expected the channel request: " + request);
            connection.getOutputStream().write(("HTTP/1.1 200 OK\r\ncontent-type: image/jpp-stream\r\n"
                    + "jpip-cnew: cid=test,transport=http,path=jpip\r\n" + framing).getBytes(StandardCharsets.US_ASCII));
            if (valid)
                connection.getOutputStream().write(new byte[]{0, 2, 0});
            return null;
        }, (uri, workers, served) -> {
            JPIPSocket client = new JPIPSocket(uri);
            try {
                ByteBuffer body = client.receive();
                if (!valid)
                    throw new AssertionError("Accepted invalid response framing: " + framing);
                if (!body.isDirect() || body.remaining() != 3 || body.get(1) != 2)
                    throw new AssertionError("Wrong body of the channel response");
            } catch (IOException e) {
                if (valid)
                    throw e;
            } finally {
                client.abort();
            }
        });
    }

    private static void testInvalidChannel(String header) throws Exception {
        Object next = exchange((connection, input) -> {
            readRequest(input);
            reply(connection, header, new byte[]{0, 2, 0});
            return input.read();
        }, (uri, workers, served) -> {
            JPIPSocket client = new JPIPSocket(uri);
            try {
                client.receive();
                throw new AssertionError("Accepted unsupported channel: " + header);
            } catch (IOException expected) {
                // A refused channel is left without a follow-up request.
            } finally {
                client.abort();
            }
        });
        if ((Integer) next != -1)
            throw new AssertionError("Invalid channel caused another request: " + header);
    }

    private static void testPipeline(String encoding) throws Exception {
        byte[] large = new byte[300000];
        for (int i = 0; i < large.length; i++)
            large[i] = (byte) (i * 31);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        boolean encoded = !encoding.equals("identity");
        if (encoded) {
            try (OutputStream output = encoding.equals("gzip") ? new GZIPOutputStream(compressed) : new DeflaterOutputStream(compressed)) {
                output.write(large);
            }
        }
        byte[] payload = encoded ? compressed.toByteArray() : large;
        exchange((connection, input) -> {
            readRequest(input);
            reply(connection, CHANNEL, new byte[]{0, 2, 0});
            String metadata = readRequest(input);
            if (!metadata.startsWith("GET /jpip?cid=test&stream=0&metareq=[*]!!&len=2000000 "))
                throw new IOException("Expected the metadata request: " + metadata);
            reply(connection, "", new byte[]{0, 4, 0});
            // Require both requests before responding, proving the client sends ahead.
            String first = readRequest(input), second = readRequest(input);
            if (!first.startsWith("GET /jpip?cid=test&stream=7&fsiz=64,65,closest&rsiz=65,66&len=2097152 ")
                    || !second.startsWith("GET /jpip?cid=test&stream=8&fsiz=64,64,closest&rsiz=64,64&len=2097152 "))
                throw new IOException("Unexpected frame requests: " + first + " | " + second);
            // A chunked body larger than the client's initial buffer.
            OutputStream out = connection.getOutputStream();
            out.write(("HTTP/1.1 200 OK\r\nContent-Type: image/jpp-stream\r\nTransfer-Encoding: chunked\r\n"
                    + (encoded ? "Content-Encoding: " + encoding + "\r\n" : "") + "\r\n").getBytes(StandardCharsets.US_ASCII));
            int chunkSize = encoded ? 7 : 100000; // Split compression framing across HTTP chunks too.
            for (int at = 0; at < payload.length; at += chunkSize) {
                int count = Math.min(chunkSize, payload.length - at);
                out.write((Integer.toHexString(count) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(payload, at, count);
                out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            }
            out.write("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            reply(connection, "", new byte[]{0, 1, 0});
            readRequest(input);
            reply(connection, "Connection: close\r\n", new byte[]{5, 6, 7, 8});
            return null;
        }, (uri, workers, served) -> {
            JPIPSocket client = new JPIPSocket(uri);
            try {
                client.receive();
                client.sendMetadata();
                if (client.receive().get(1) != 4)
                    throw new AssertionError("Wrong metadata response");
                client.sendFrame(7, 64, 65, 1);
                client.sendFrame(8, 64, 64, 0);
                if (!client.receive().equals(ByteBuffer.wrap(large)))
                    throw new AssertionError("First response is not the large body");
                ByteBuffer second = client.receive();
                if (second.remaining() != 3 || second.get(1) != 1)
                    throw new AssertionError("Second response is not the short body");
                client.sendFrame(7, 64, 64, 0);
                if (!client.receive().equals(ByteBuffer.wrap(new byte[]{5, 6, 7, 8})))
                    throw new AssertionError("Sequential request after draining failed");
                if (!client.isClosed())
                    throw new AssertionError("Server close was ignored");
            } finally {
                client.abort();
            }
        });
    }

    private static void testInterruptedHandshake() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        Object next = exchange((connection, input) -> {
            readRequest(input);
            requestReceived.countDown();
            // Do not reply to cnew: only the interrupt can release the client.
            return input.read();
        }, (uri, workers, served) -> {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            FutureTask<Void> load = new FutureTask<>(() -> {
                try {
                    new JPIPSocket(uri).receive();
                    failure.set(new AssertionError("Stalled handshake completed"));
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted())
                        failure.set(e);
                } catch (Throwable e) {
                    failure.set(e);
                }
                return null;
            });
            Thread reader = Thread.ofVirtual().start(load);
            try {
                if (!requestReceived.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Handshake did not reach server");
                load.cancel(true);
                reader.join(2000);
                if (reader.isAlive())
                    throw new AssertionError("Interrupt did not release the handshake");
                if (failure.get() != null)
                    throw new AssertionError("Handshake cancellation failed", failure.get());
            } finally {
                reader.interrupt();
                reader.join(2000);
            }
        });
        if ((Integer) next != -1)
            throw new AssertionError("Handshake cancellation did not close TCP");
    }

    private static void testClose(boolean abort) throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        exchange((connection, input) -> {
            readRequest(input);
            reply(connection, CHANNEL, new byte[]{0, 2, 0});
            if (abort) {
                readRequest(input);
                readRequest(input);
                requestReceived.countDown();
                // Leave the response pending until the client aborts the connection.
            }
            return input.readLine();
        }, (uri, workers, served) -> {
            JPIPSocket client = new JPIPSocket(uri);
            try {
                client.receive();
                if (abort) {
                    Future<?> pending = workers.submit(() -> {
                        try {
                            client.sendFrame(0, 64, 64, 0);
                            client.sendFrame(1, 64, 64, 0);
                            client.receive();
                            throw new AssertionError("Stalled response completed successfully");
                        } catch (IOException expected) {
                            // Closing TCP must release the blocked read.
                        }
                        return null;
                    });
                    if (!requestReceived.await(5, TimeUnit.SECONDS))
                        throw new AssertionError("Request did not reach server");
                    client.abort();
                    pending.get(5, TimeUnit.SECONDS);
                    if (served.get(5, TimeUnit.SECONDS) != null)
                        throw new AssertionError("Abort wrote another request");
                } else {
                    client.close();
                    String request = (String) served.get(5, TimeUnit.SECONDS);
                    if (request == null || !request.startsWith("GET /jpip?cclose=test&"))
                        throw new AssertionError("Missing graceful channel close: " + request);
                }
                client.abort(); // Repeated abort is harmless.
                if (!client.isClosed())
                    throw new AssertionError("Socket remains open");
            } finally {
                client.abort();
            }
        });
    }

    private static void reply(Socket connection, String headers, byte[] body) throws IOException {
        connection.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: image/jpp-stream\r\n"
                + headers + "Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        connection.getOutputStream().write(body);
    }

    private static String readRequest(BufferedReader input) throws IOException {
        String request = input.readLine();
        String line = request;
        while (line != null) {
            if (line.isEmpty())
                return request;
            line = input.readLine();
        }
        throw new IOException("Connection ended before request headers");
    }
}
