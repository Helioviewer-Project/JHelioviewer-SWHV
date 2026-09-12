package org.helioviewer.jhv.view.j2k.jpip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class JPIPSocketTest {

    public static void main(String[] arguments) throws Exception {
        testResponse("content-length: 3\r\n\r\n", true);
        testResponse("Content-Length: -1\r\n\r\n", false);
        testResponse("Content-Length: 3\r\n", false);
        for (String cnew : new String[]{"cid=test,transport=http", "transport=http,path=jpip",
                "cid=test,path=jpip", "cid=test,transport=http-tcp,path=jpip"})
            testInvalidChannel(cnew);
        testPipeline();
        testInterruptedHandshake();
        testClose(false);
        testClose(true);
        System.out.println("PASS: graceful close, response abort and interrupted constructor handshake");
    }

    private static void testInvalidChannel(String cnew) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            Future<Integer> server = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII));
                    readRequest(input);
                    reply(connection, "JPIP-cnew: " + cnew + "\r\n", 2);
                    return input.read();
                }
            });
            try {
                JPIPSocket client = new JPIPSocket(
                        URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), null);
                client.abort();
                throw new AssertionError("Accepted unsupported channel: " + cnew);
            } catch (IOException expected) {
                // Constructor failure must close TCP without sending a follow-up request.
            }
            if (server.get(5, TimeUnit.SECONDS) != -1)
                throw new AssertionError("Invalid channel caused another request: " + cnew);
        }
    }

    private static void testPipeline() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            Future<?> server = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII));
                    readRequest(input);
                    reply(connection, "JPIP-cnew: cid=test,transport=http,path=jpip\r\n", 2);
                    // Require both requests before responding, proving the client sends ahead.
                    readRequest(input);
                    readRequest(input);
                    reply(connection, "", 3); // Partial response, followed by a complete one.
                    reply(connection, "", 2);
                    readRequest(input);
                    reply(connection, "Connection: close\r\n", 2);
                }
                return null;
            });
            JPIPSocket client = new JPIPSocket(URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), null);
            try {
                client.sendFrame(0, "64,64");
                client.sendFrame(1, "64,64");
                if (client.pendingCount() != 2)
                    throw new AssertionError("Missing pending requests");
                JPIPSocket.FrameResponse first = client.receiveFrame(null);
                if (first.frame() != 0 || first.complete() || client.pendingCount() != 1)
                    throw new AssertionError("First response should belong to frame 0 and be partial");
                JPIPSocket.FrameResponse second = client.receiveFrame(null);
                if (second.frame() != 1 || !second.complete() || client.pendingCount() != 0)
                    throw new AssertionError("Second response should belong to frame 1 and be complete");
                client.sendFrame(0, "64,64");
                if (!client.receiveFrame(null).complete())
                    throw new AssertionError("Sequential request after draining failed");
                if (!client.isClosed())
                    throw new AssertionError("Server close was ignored");
            } finally {
                client.abort();
            }
            server.get(5, TimeUnit.SECONDS);
        }
    }

    private static void reply(Socket connection, String headers, int reason) throws IOException {
        connection.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: image/jpp-stream\r\n"
                + headers + "Content-Length: 3\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        connection.getOutputStream().write(new byte[]{0, (byte) reason, 0});
    }

    private static void testInterruptedHandshake() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            CountDownLatch requestReceived = new CountDownLatch(1);
            Future<Integer> server = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII));
                    readRequest(input);
                    requestReceived.countDown();
                    // Do not reply to cnew: the client has no constructed JPIPSocket to abort.
                    return input.read();
                }
            });
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread reader = Thread.ofVirtual().start(() -> {
                try {
                    JPIPSocket client = new JPIPSocket(
                            URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), null);
                    client.abort();
                    failure.set(new AssertionError("Stalled handshake completed"));
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted())
                        failure.set(e);
                } catch (Throwable e) {
                    failure.set(e);
                }
            });
            try {
                if (!requestReceived.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("Handshake did not reach server");
                reader.interrupt();
                reader.join(2000);
                if (reader.isAlive())
                    throw new AssertionError("Interrupt did not release the handshake");
                if (failure.get() != null)
                    throw new AssertionError("Handshake cancellation failed", failure.get());
                if (server.get(5, TimeUnit.SECONDS) != -1)
                    throw new AssertionError("Handshake cancellation did not close TCP");
            } finally {
                reader.interrupt();
                reader.join(2000);
            }
        }
    }

    private static void testResponse(String framing, boolean valid) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            Future<?> server = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    readRequest(new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII)));
                    connection.getOutputStream().write(("HTTP/1.1 200 OK\r\n"
                            + "content-type: image/jpp-stream\r\n"
                            + "jpip-cnew: cid=test,transport=http,path=jpip\r\n"
                            + framing).getBytes(StandardCharsets.US_ASCII));
                    if (valid)
                        connection.getOutputStream().write(new byte[]{0, 2, 0});
                }
                return null;
            });
            try {
                JPIPSocket client = new JPIPSocket(
                        URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), null);
                client.abort();
                if (!valid)
                    throw new AssertionError("Accepted invalid response framing: " + framing);
            } catch (IOException e) {
                if (valid)
                    throw e;
            }
            server.get(5, TimeUnit.SECONDS);
        }
    }

    private static void testClose(boolean abort) throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            listener.setSoTimeout(5000);
            CountDownLatch requestReceived = new CountDownLatch(1);
            Future<String> received = workers.submit(() -> {
                try (Socket connection = listener.accept()) {
                    connection.setSoTimeout(5000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.US_ASCII));
                    readRequest(input);
                    connection.getOutputStream().write(("HTTP/1.1 200 OK\r\n"
                            + "Content-Type: image/jpp-stream\r\n"
                            + "JPIP-cnew: cid=test,transport=http,path=jpip\r\n"
                            + "Content-Length: 3\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    connection.getOutputStream().write(new byte[]{0, 2, 0});
                    if (abort) {
                        readRequest(input);
                        readRequest(input);
                        requestReceived.countDown();
                        // Leave the response pending until the client aborts the connection.
                    }
                    return input.readLine();
                }
            });
            JPIPSocket client = new JPIPSocket(URI.create("jpip://127.0.0.1:" + listener.getLocalPort() + "/test"), null);
            try {
                if (abort) {
                    Future<?> pending = workers.submit(() -> {
                        try {
                            client.sendFrame(0, "64,64");
                            client.sendFrame(1, "64,64");
                            client.receiveFrame(null);
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
                    if (received.get(5, TimeUnit.SECONDS) != null)
                        throw new AssertionError("Abort wrote another request");
                } else {
                    client.close();
                    String request = received.get(5, TimeUnit.SECONDS);
                    if (request == null || !request.startsWith("GET /jpip?cclose=test&"))
                        throw new AssertionError("Missing graceful channel close: " + request);
                }
                client.abort(); // Repeated abort is harmless.
                if (!client.isClosed())
                    throw new AssertionError("Socket remains open");
            } finally {
                client.abort();
            }
        }
    }

    private static void readRequest(BufferedReader input) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isEmpty())
                return;
        }
        throw new IOException("Connection ended before request headers");
    }
}
