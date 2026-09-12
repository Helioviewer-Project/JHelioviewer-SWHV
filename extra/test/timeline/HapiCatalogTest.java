package org.helioviewer.jhv.timelines.band;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.io.Directories;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;

public final class HapiCatalogTest {
    static Object[] load(String... urls) throws Exception {
        Class<?> endpointType = Class.forName(BandReaderHapi.class.getName() + "$CatalogEndpoint");
        Constructor<?> constructor = endpointType.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        Object endpoints = Array.newInstance(endpointType, urls.length);
        for (int i = 0; i < urls.length; i++)
            Array.set(endpoints, i, constructor.newInstance("Test" + i, urls[i]));
        Method load = BandReaderHapi.class.getDeclaredMethod("loadCatalogs", endpoints.getClass());
        load.setAccessible(true);
        return (Object[]) load.invoke(null, endpoints);
    }

    public static void main(String[] args) throws Exception {
        Directories.createCacheDirs();
        int workers = 8;
        int datasetCount = Math.max(17, workers + 1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch firstWave = new CountDownLatch(workers);
        CountDownLatch interruptedStarted = new CountDownLatch(workers);
        CountDownLatch releaseInterrupted = new CountDownLatch(1);
        AtomicInteger interruptedCalls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try (ExecutorService handlers = Executors.newCachedThreadPool()) {
            server.setExecutor(handlers);
            server.createContext("/", exchange -> {
                try (exchange) {
                    String path = exchange.getRequestURI().getPath();
                    String body;
                    if (path.endsWith("catalog")) {
                        JSONArray ids = new JSONArray();
                        for (int i = 0; i < datasetCount; i++)
                            ids.put(new JSONObject().put("id", "d" + i).put("title", "Dataset " + i));
                        body = new JSONObject().put("HAPI", "3.1").put("status", new JSONObject().put("code", 1200).put("message", "OK")).put("catalog", ids).toString();
                    } else {
                        peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                        calls.incrementAndGet();
                        try {
                            if (path.startsWith("/interrupt/")) {
                                interruptedCalls.incrementAndGet();
                                interruptedStarted.countDown();
                                releaseInterrupted.await(5, TimeUnit.SECONDS);
                            }
                            firstWave.countDown();
                            if (!firstWave.await(5, TimeUnit.SECONDS))
                                throw new AssertionError("Requests did not execute concurrently");
                            String id = exchange.getRequestURI().getQuery().split("=")[1];
                            int index = Integer.parseInt(id.substring(1));
                            Thread.sleep((datasetCount - index) * 3L); // Complete out of submission order.
                            body = index == 5 || path.startsWith("/empty/") ? "{}" : """
                                    {"HAPI":"3.1","status":{"code":1200,"message":"OK"},"parameters":[
                                      {"name":"Time","type":"isotime","length":24,"units":"UTC"},
                                      {"name":"value","type":"double","units":"nT"}]}
                                    """;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } finally {
                            active.decrementAndGet();
                        }
                    }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Cache-Control", "no-store");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            try {
                Object[] catalogs = load(base + "/first/", base + "/empty/", base + "/last/");
                if (catalogs.length != 3 || catalogs[1] != null)
                    throw new AssertionError("Failed endpoint was not retained in its position");
                for (int endpoint : new int[]{0, 2}) {
                    Method datasetsMethod = catalogs[endpoint].getClass().getDeclaredMethod("datasets");
                    datasetsMethod.setAccessible(true);
                    BandDataset[] datasets = (BandDataset[]) datasetsMethod.invoke(catalogs[endpoint]);
                    if (datasets.length != datasetCount - 1)
                        throw new AssertionError("Partial failure lost valid datasets");
                    for (int i = 0; i < datasets.length; i++) {
                        int expected = i < 5 ? i : i + 1;
                        if (!datasets[i].title().equals("Dataset " + expected) || datasets[i].bandTypes().size() != 1)
                            throw new AssertionError("Dataset order or parameters changed");
                    }
                }
                if (peak.get() != workers || calls.get() != 3 * datasetCount)
                    throw new AssertionError("Unexpected concurrency or request count: " + peak + "/" + calls);
                if (Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().equals("HAPI-Catalog-Request")))
                    throw new AssertionError("Catalog request workers survived load completion");
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread coordinator = new Thread(() -> {
                    try {
                        load(base + "/interrupt/");
                        failure.set(new AssertionError("Interrupted catalog load succeeded"));
                    } catch (InvocationTargetException e) {
                        if (!(e.getCause() instanceof InterruptedException))
                            failure.set(e);
                    } catch (Exception e) {
                        failure.set(e);
                    }
                });
                coordinator.start();
                try {
                    if (!interruptedStarted.await(5, TimeUnit.SECONDS))
                        throw new AssertionError("Interrupted requests did not start");
                    coordinator.interrupt();
                    // Wait for invokeAll cancellation before letting active platform reads finish.
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    boolean closing = false;
                    while (!closing && System.nanoTime() < deadline) {
                        for (StackTraceElement frame : coordinator.getStackTrace())
                            closing |= frame.getClassName().equals("java.util.concurrent.ExecutorService")
                                    && frame.getMethodName().equals("close");
                        if (!closing)
                            Thread.sleep(1);
                    }
                    if (!closing)
                        throw new AssertionError("Interrupted coordinator did not reach executor cleanup");
                    releaseInterrupted.countDown();
                    coordinator.join(5000);
                    if (coordinator.isAlive() || failure.get() != null || interruptedCalls.get() != workers)
                        throw new AssertionError("Interruption did not cancel queued requests", failure.get());
                    if (Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().equals("HAPI-Catalog-Request")))
                        throw new AssertionError("Interrupted load leaked workers");
                } finally {
                    releaseInterrupted.countDown();
                    coordinator.interrupt();
                    coordinator.join(5000);
                }
                System.out.println("PASS: HAPI catalog concurrency, endpoint/dataset ordering, partial failures, interruption and worker cleanup");
            } finally {
                server.stop(0);
            }
        }
    }

    private HapiCatalogTest() {}
}
