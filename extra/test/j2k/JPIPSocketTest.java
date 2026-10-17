package org.helioviewer.jhv.source.jpip;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.ProxySettings;

public final class JPIPSocketTest {

    private static final String CHANNEL = "JPIP-cnew: cid=test,transport=http,path=jpip\r\n";

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) {
            testProxy(arguments[0], arguments[1]);
            return;
        }
        ProxySettings.init();
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
        for (boolean refused : new boolean[]{false, true})
            testProxyFailover(refused);
        System.out.println("PASS: channel opening, plain/gzip/deflate chunked bodies in request order, buffer growth, graceful close, abort and cancelled handshake");
    }

    private static void testProxyFailover(boolean refused) throws Exception {
        ProxySelector original = ProxySelector.getDefault();
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ServerSocket first = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ServerSocket second = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            Proxy primary = new Proxy(Proxy.Type.HTTP, first.getLocalSocketAddress());
            Proxy backup = new Proxy(Proxy.Type.HTTP, second.getLocalSocketAddress());
            second.close();
            AtomicInteger closed = new AtomicInteger();
            Future<?> serving = workers.submit(() -> {
                while (!first.isClosed()) {
                    try (Socket connection = first.accept()) {
                        connection.setSoTimeout(5000);
                        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                        if (!readRequest(input).startsWith("CONNECT "))
                            throw new AssertionError("Expected a proxy tunnel request");
                        if (refused)
                            connection.getOutputStream().write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                                    + "Proxy-Authenticate: Basic realm=\"cleanup\"\r\nContent-Length: 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        // Leave the connection open so EOF proves the client closed it.
                        if (input.read() != -1)
                            throw new AssertionError("Failed proxy connection stayed open");
                        closed.incrementAndGet();
                    } catch (SocketException e) {
                        if (!first.isClosed())
                            throw e;
                    }
                }
                return null;
            });
            URI destination = URI.create("http://127.0.0.1:" + listener.getLocalPort() + "/test");
            listener.setSoTimeout(5000);
            try {
                for (List<Proxy> routes : List.of(List.of(Proxy.NO_PROXY), List.of(primary, Proxy.NO_PROXY), List.of(primary, backup))) {
                    boolean succeeds = routes.getLast().type() == Proxy.Type.DIRECT;
                    List<SocketAddress> reported = new ArrayList<>();
                    ProxySelector.setDefault(new ProxySelector() {
                        @Override
                        public List<Proxy> select(URI uri) {
                            if (!uri.equals(destination))
                                throw new AssertionError("Wrong selection URI: " + uri);
                            return routes;
                        }

                        @Override
                        public void connectFailed(URI uri, SocketAddress address, IOException failure) {
                            if (!uri.equals(destination))
                                throw new AssertionError("Wrong failure URI: " + uri);
                            if (refused && address.equals(primary.address()) && !failure.toString().contains("407"))
                                throw new AssertionError("Expected proxy refusal", failure);
                            reported.add(address);
                        }
                    });
                    try (Socket connected = ProxySettings.connectJpipSocket(destination, 2000, 1234); Socket peer = listener.accept()) {
                        if (!succeeds || connected.getSoTimeout() != 1234 || !connected.getTcpNoDelay() || !connected.getKeepAlive() || peer.getPort() != connected.getLocalPort())
                            throw new AssertionError("Wrong fallback socket or socket options");
                    } catch (IOException e) {
                        if (succeeds)
                            throw e;
                    }
                    List<SocketAddress> expected = new ArrayList<>();
                    for (Proxy route : routes) {
                        if (route.type() != Proxy.Type.DIRECT)
                            expected.add(route.address());
                    }
                    if (!reported.equals(expected))
                        throw new AssertionError("Wrong failure reports: " + reported);
                }
            } finally {
                first.close();
            }
            serving.get(10, TimeUnit.SECONDS);
            if (closed.get() != 2)
                throw new AssertionError("Failed proxy sockets were not closed: " + closed.get());
        } finally {
            ProxySelector.setDefault(original);
        }
        System.out.println("PASS: direct connections, proxy failover, direct fallback, exhausted routes, socket options and cleanup after " + (refused ? "407 refusal" : "read timeout"));
    }

    // Each proxy profile runs in its own JVM: proxy selection and authentication
    // policy are initialized once, just as at application startup.
    private static void testProxy(String type, String profile) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        boolean http = type.equals("http");
        boolean allowed = !profile.equals("missing") && !profile.equals("wrong");
        boolean bypass = profile.equals("bypass");
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor()) {
            String host = profile.equals("hostname") ? "localhost" : "127.0.0.1";
            System.setProperty(http ? "http.proxyHost" : "socksProxyHost", host);
            System.setProperty(http ? "http.proxyPort" : "socksProxyPort", Integer.toString(listener.getLocalPort()));
            System.setProperty("http.nonProxyHosts", bypass ? "127.*" : "");
            System.setProperty("socksNonProxyHosts", "");
            if (profile.equals("mixed")) {
                System.setProperty("socksProxyHost", "127.0.0.1");
                System.setProperty("socksProxyPort", "1");
            }
            if (!profile.equals("missing")) {
                Settings.setProperty("proxy.username", "proxy-user");
                ProxySettings.setPassword(profile.equals("wrong") || profile.equals("override") ? "wrong" : "proxy-pass");
            }
            if (profile.equals("override") || profile.equals("partial"))
                System.setProperty(http ? "http.proxyUser" : "java.net.socks.username", profile.equals("partial") ? "ignored" : "proxy-user");
            if (profile.equals("override"))
                System.setProperty(http ? "http.proxyPassword" : "java.net.socks.password", "proxy-pass");
            if (profile.equals("blocked"))
                System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "Basic");
            ProxySettings.init();
            AtomicInteger failures = new AtomicInteger();
            if (profile.equals("failover")) {
                ProxySelector original = ProxySelector.getDefault();
                try (ServerSocket refused = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                    Proxy primary = new Proxy(http ? Proxy.Type.HTTP : Proxy.Type.SOCKS, refused.getLocalSocketAddress());
                    ProxySelector.setDefault(new ProxySelector() {
                        @Override
                        public List<Proxy> select(URI uri) {
                            return List.of(primary, original.select(uri).getFirst());
                        }

                        @Override
                        public void connectFailed(URI uri, SocketAddress address, IOException failure) {
                            if (!address.equals(primary.address()))
                                throw new AssertionError("Reported the wrong proxy: " + address);
                            failures.incrementAndGet();
                        }
                    });
                }
            }
            if (Authenticator.requestPasswordAuthentication(host, null, listener.getLocalPort(), "http", "test", "Basic", null,
                    Authenticator.RequestorType.SERVER) != null)
                throw new AssertionError("Proxy credentials were supplied to an origin server");

            int[] delivered = {0};
            Future<?> serving = worker.submit(() -> {
                while (!listener.isClosed()) {
                    try (Socket connection = listener.accept()) {
                        connection.setSoTimeout(5000);
                        if (serveProxy(connection, bypass ? Proxy.Type.DIRECT : http ? Proxy.Type.HTTP : Proxy.Type.SOCKS))
                            delivered[0]++;
                    } catch (SocketException e) {
                        if (!listener.isClosed())
                            throw e;
                    }
                }
                return null;
            });
            try {
                for (boolean jpip : new boolean[]{false, true}) {
                    int previousFailures = failures.get();
                    boolean succeeded = false;
                    URI target = URI.create((jpip ? "jpip" : "http") + "://127.0.0.1:" + (bypass ? listener.getLocalPort() : 1) + "/test");
                    try {
                        if (jpip) {
                            JPIPSocket client = new JPIPSocket(target);
                            try {
                                if (!client.receive().equals(ByteBuffer.wrap(new byte[]{1, 2, 3})))
                                    throw new AssertionError("Wrong proxied JPIP body");
                            } finally {
                                client.close();
                            }
                        } else {
                            try (NetClient client = NetClient.of(target, false, NetClient.NetCache.BYPASS)) {
                                if (!Arrays.equals(client.getStream().readAllBytes(), new byte[]{1, 2, 3}))
                                    throw new AssertionError("Wrong proxied HTTP body");
                            }
                        }
                        succeeded = true;
                    } catch (IOException expected) {
                        if (allowed && !(jpip && profile.equals("blocked")))
                            throw expected;
                    }
                    if (succeeded != (allowed && !(jpip && profile.equals("blocked"))))
                        throw new AssertionError("Wrong proxy result for " + type + '/' + profile + (jpip ? " JPIP" : " HTTP"));
                    if (profile.equals("failover") && (jpip ? failures.get() - previousFailures != 1 : failures.get() == previousFailures))
                        throw new AssertionError("Wrong failed-proxy reports for " + (jpip ? "JPIP" : "HTTP"));
                }
            } finally {
                listener.close();
            }
            serving.get(10, TimeUnit.SECONDS);
            int expected = !allowed ? 0 : profile.equals("blocked") ? 1 : 2;
            if (delivered[0] != expected)
                throw new AssertionError("Proxy did not deliver the expected requests: " + delivered[0]);
        }
        System.out.println("PASS: " + type + " proxy, " + profile + " profile, general HTTP and JPIP connections");
    }

    private static boolean serveProxy(Socket connection, Proxy.Type type) throws IOException {
        if (type == Proxy.Type.SOCKS && !authenticateSocks(connection))
            return false;
        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
        String request = input.readLine();
        if (request == null)
            return false;
        String authorization = null;
        String line;
        while ((line = input.readLine()) != null && !line.isEmpty()) {
            if (line.regionMatches(true, 0, "Proxy-Authorization:", 0, 20))
                authorization = line.substring(20).trim();
        }
        if (type == Proxy.Type.DIRECT && (!request.startsWith("GET /") || authorization != null))
            throw new IOException("Excluded destination was sent through a proxy: " + request);
        if (type == Proxy.Type.HTTP && !("Basic " + Base64.getEncoder().encodeToString("proxy-user:proxy-pass".getBytes(StandardCharsets.US_ASCII))).equals(authorization)) {
            connection.getOutputStream().write(("HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"JHV test\"\r\n"
                    + "Content-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            return false;
        }
        if (request.startsWith("CONNECT ")) {
            connection.getOutputStream().write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            readRequest(input);
        }
        reply(connection, CHANNEL + "Connection: close\r\n", new byte[]{1, 2, 3});
        return true;
    }

    private static boolean authenticateSocks(Socket connection) throws IOException {
        InputStream input = connection.getInputStream();
        OutputStream output = connection.getOutputStream();
        if (input.read() != 5)
            throw new IOException("Expected SOCKS5 greeting");
        input.readNBytes(input.read());
        output.write(new byte[]{5, 2});
        if (input.read() != 1)
            throw new IOException("Expected SOCKS password authentication");
        String user = new String(input.readNBytes(input.read()), StandardCharsets.UTF_8);
        String password = new String(input.readNBytes(input.read()), StandardCharsets.UTF_8);
        boolean accepted = user.equals("proxy-user") && password.equals("proxy-pass");
        output.write(new byte[]{1, (byte) (accepted ? 0 : 1)});
        if (!accepted)
            return false;
        byte[] request = input.readNBytes(4);
        int size = switch (request[3]) {
            case 1 -> 4;
            case 3 -> input.read();
            case 4 -> 16;
            default -> throw new IOException("Unknown SOCKS address type");
        };
        input.readNBytes(size + 2);
        output.write(new byte[]{5, 0, 0, 1, 127, 0, 0, 1, 0, 0});
        return true;
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
