package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongUnaryOperator;

import javax.swing.event.TableModelEvent;

import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.management.ThreadMXBean;
import com.sun.net.httpserver.HttpServer;

public final class TimelineDataTest {

    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;
    private static Instrumentation instrumentation;

    public static void premain(String options, Instrumentation agent) {
        instrumentation = agent;
    }

    private static final long START = 1_700_000_000_123L;
    private static final YAxis AXIS = new YAxis(0, 100, YAxis.generateScale("linear", ""));

    public static void main(String[] args) throws Exception {
        Directories.createCacheDirs();
        checkEmpty();
        checkOrderingAndGaps();
        checkUnsortedAndEqualTimestamps();
        checkFullSampleCount();
        checkLayerCreation();
        checkSavedState();
        checkExportImport();
        checkHapiRequests();
        System.out.println("Timeline data tests passed");
        if (args.length > 0 && "--benchmark".equals(args[0])) {
            EventQueue.invokeAndWait(() -> {
                try {
                    benchmark();
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static void checkSavedState() throws Exception {
        EventQueue.invokeAndWait(() -> {
            TimelineLayers layers = new TimelineLayers();
            try {
                BandType type = new BandType(new JSONObject().put("name", "saved"));
                Band original = layers.addBands(List.of(type), true).getFirst();
                original.addToCache(new float[]{12, 34}, new long[]{START, START + 100});
                Object cache = field(Band.class, original, "bandCache");
                JSONObject state = new JSONObject();
                original.serialize(state);
                check(state.getBoolean("fullResolution"), "Saved state omits full resolution");
                Band restored = (Band) Band.deserialize(new JSONObject(state.toString()));
                check(restored.isFullResolution(), "Restored state loses full resolution");
                restored.setDataColor(Color.RED);
                layers.restore(List.of(restored));
                check(TimelineLayers.get().getFirst() == original, "Matching resolution discards the existing band");
                check(field(Band.class, original, "bandCache") == cache && original.hasData(), "Matching resolution discards cached data");
                check(original.getDataColor().equals(Color.RED), "Reusing a band loses restored appearance");

                state.put("fullResolution", false);
                Band standard = (Band) Band.deserialize(state);
                layers.restore(List.of(standard));
                check(TimelineLayers.get().getFirst() == standard && !standard.isFullResolution(), "Different resolution reuses the old band");
                check(!original.hasData() && !standard.hasData(), "Different resolution retains the old cache");
                JSONObject standardState = new JSONObject();
                standard.serialize(standardState);
                check(!standardState.getBoolean("fullResolution"), "Standard resolution is not saved");
                state.remove("fullResolution");
                Band oldState = (Band) Band.deserialize(state);
                check(!oldState.isFullResolution(), "State without a resolution setting changes the default");
                layers.restore(List.of(oldState));
                check(TimelineLayers.get().getFirst() == standard, "Default state does not reuse standard-resolution data");

                state.put("fullResolution", true);
                Band full = (Band) Band.deserialize(state);
                layers.restore(List.of(full));
                check(TimelineLayers.get().getFirst() == full && layers.getRowCount() == 1, "Restoring full resolution duplicates the layer");
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                for (TimelineLayer layer : List.copyOf(TimelineLayers.get()))
                    layers.remove(layer);
            }
        });
    }

    private static void checkExportImport() throws Exception {
        EventQueue.invokeAndWait(() -> {
            Band band = new Band(new BandType(new JSONObject().put("name", "export")), true);
            long previousStart = DrawController.selectedAxis.start();
            long previousEnd = DrawController.selectedAxis.end();
            try {
                DrawController.setSelectedInterval(START, START + 60_000);
                long[] dates = {START, START + 1, START + 100, START + 877, START + 1000};
                float[] values = {1.5f, 2.25f, YAxis.BLANK, 3, 6};
                band.addToCache(values, dates);
                JSONObject document = new JSONObject(band.toJson().toString());
                JSONObject exported = document.getJSONArray("org.helioviewer.jhv.request.timeline").getJSONObject(0);
                check(exported.getBoolean("fullResolution"), "Export loses the resolution setting");
                check(exported.getDouble("multiplier") == 1.5, "Export fixture does not exercise scaled values");
                checkSamples(asSamples(readExport(exported)), dates, values);

                JSONObject oldExport = new JSONObject().put("bandType", new JSONObject().put("name", "old export"))
                        .put("multiplier", 2).put("data", new JSONArray().put(new JSONArray().put(1_700_000_000L).put(1.25)));
                checkSamples(asSamples(readExport(oldExport)), new long[]{1_700_000_000_000L}, new float[]{2.5f});
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                band.remove();
                DrawController.setSelectedInterval(previousStart, previousEnd);
            }
        });
    }

    private static BandData readExport(JSONObject exported) throws Exception {
        Constructor<?> constructor = Class.forName(BandImporter.class.getName() + "$BandLoad").getDeclaredConstructor(JSONObject.class);
        constructor.setAccessible(true);
        Callable<?> load = (Callable<?>) constructor.newInstance(exported);
        List<?> data = (List<?>) load.call();
        check(data.size() == 1, "Import lost the exported band");
        return (BandData) data.getFirst();
    }

    private static void checkLayerCreation() throws Exception {
        EventQueue.invokeAndWait(() -> {
            TimelineLayers layers = new TimelineLayers();
            BandType first = new BandType(new JSONObject().put("name", "first"));
            BandType second = new BandType(new JSONObject().put("name", "second"));
            BandType third = new BandType(new JSONObject().put("name", "third"));
            Band existing = layers.addBands(List.of(first)).getFirst();
            List<TableModelEvent> events = new ArrayList<>();
            layers.addTableModelListener(events::add);
            try {
                List<Band> added = layers.addBands(List.of(first, second, second, third, first), true);
                check(layers.getRowCount() == 3, "Existing selection adds another row");
                check(added.equals(List.of(existing, added.get(1), added.get(1), added.get(3), existing)),
                        "Overlapping selections lose their order or create duplicate bands");
                check(added.get(0) == existing && !existing.isFullResolution(), "Existing layer was changed");
                check(added.get(1).isFullResolution() && added.get(3).isFullResolution(), "New layers lost the selection setting");
                check(events.size() == 1 && events.getFirst().getType() == TableModelEvent.INSERT
                        && events.getFirst().getFirstRow() == 1 && events.getFirst().getLastRow() == 2,
                        "Adding layers does not report exactly the newly inserted rows");
                events.clear();
                check(layers.addBands(List.of(second), false).getFirst() == added.get(1), "Re-adding replaces a layer");
                check(added.get(1).isFullResolution() && layers.getRowCount() == 3, "Re-adding changes resolution or row count");
                check(layers.addBands(List.of(), true).isEmpty(), "Empty selection returns bands");
                check(events.isEmpty(), "Existing or empty selections notify a table change");
            } finally {
                for (TimelineLayer layer : List.copyOf(TimelineLayers.get()))
                    layers.remove(layer);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void checkHapiRequests() throws Exception {
        long start = Instant.parse("2024-01-01T00:00:00.123Z").toEpochMilli();
        long[] dates = new long[13];
        float[] detail = new float[13];
        ByteBuffer binary = ByteBuffer.allocate(13 * (24 + 3 * Double.BYTES)).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < dates.length; i++) {
            dates[i] = start + i * 10_000L;
            detail[i] = i == 3 ? YAxis.BLANK : 100 + i;
            binary.put(Instant.ofEpochMilli(dates[i]).toString().getBytes(StandardCharsets.US_ASCII));
            binary.putDouble(i).putDouble(i == 3 ? -1 : detail[i]).putDouble(200 + i);
        }
        String info = """
                {"HAPI":"3.1", "status":{"code":1200,"message":"OK"},
                 "startDate":"2023-01-01T00:00:00Z", "stopDate":"2025-01-01T00:00:00Z",
                 "parameters":[
                   {"name":"Time","type":"isotime","length":24,"units":"UTC","fill":null},
                   {"name":"mean","type":"double","units":"nT","fill":"-1"},
                   {"name":"detail","type":"double","units":"nT","fill":"-1"},
                   {"name":"bar","type":"double","units":"nT","fill":"-1","jhvparams":{"plotType":"bar","barWidth":10}}
                 ]}
                """;
        String catalogResponse = """
                {"HAPI":"3.1", "status":{"code":1200,"message":"OK"},
                 "catalog":[{"id":"test","title":"Test"}]}
                """;
        Map<String, byte[]> responses = Map.of(
                "/catalog", catalogResponse.getBytes(StandardCharsets.UTF_8),
                "/info", info.getBytes(StandardCharsets.UTF_8), "/data", binary.array());
        AtomicInteger dataRequests = new AtomicInteger();
        AtomicBoolean holdDownloads = new AtomicBoolean();
        CountDownLatch downloadStarted = new CountDownLatch(1);
        CountDownLatch releaseDownload = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                if ("/data".equals(path)) {
                    dataRequests.incrementAndGet();
                    if (holdDownloads.get()) {
                        downloadStarted.countDown();
                        try {
                            if (!releaseDownload.await(10, TimeUnit.SECONDS)) {
                                exchange.sendResponseHeaders(504, -1);
                                return;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
                    if (!query.contains("parameters=mean,detail,bar")) {
                        exchange.sendResponseHeaders(400, -1);
                        return;
                    }
                }
                byte[] body = responses.get(path);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
        Map<Object, Object> catalogs = (Map<Object, Object>) field(BandReaderHapi.class, null, "catalogs");
        try {
            Method getCatalog = BandReaderHapi.class.getDeclaredMethod("getCatalog", String.class);
            getCatalog.setAccessible(true);
            Object catalog = getCatalog.invoke(null, "http://127.0.0.1:" + server.getAddress().getPort() + "/");
            Object endpoint = ((Object[]) field(BandReaderHapi.class, null, "catalogEndpoints"))[0];
            catalogs.put(endpoint, catalog);
            BandDataset[] datasets = (BandDataset[]) field(catalog.getClass(), catalog, "datasets");
            List<BandType> types = datasets[0].bandTypes();
            check(types.size() == 3, "Fixture parameters missing from catalog");
            Map<BandType, Boolean> resolutions = new HashMap<>();
            types.forEach(type -> resolutions.put(type, false));
            resolutions.put(types.get(1), true);
            Callable<List<BandData>> request = BandReaderHapi.dataRequest(resolutions, start, start + 130_000);
            // A submitted request must retain its choices independently of subsequent caller changes.
            resolutions.put(types.get(1), false);
            List<BandData> mixed = request.call();
            check(dataRequests.get() == 1, "Mixed resolutions split the grouped request");
            check(mixed.size() == 3, "Grouped response lost a parameter");
            checkSamples(asSamples(mixed.get(0)), new long[]{start - 123, start - 123 + 60_000, start - 123 + 120_000},
                    new float[]{2.5f, 8.5f, 12});
            checkSamples(asSamples(mixed.get(1)), dates, detail);
            float[] bars = new float[13];
            for (int i = 0; i < bars.length; i++)
                bars[i] = 200 + i;
            checkSamples(asSamples(mixed.get(2)), dates, bars);

            List<BandData> defaults = BandReaderHapi.dataRequest(resolutions, start, start + 130_000).call();
            checkSamples(asSamples(defaults.get(0)), mixed.get(0).dates(), mixed.get(0).values());
            checkSamples(asSamples(defaults.get(1)), mixed.get(0).dates(), new float[]{102.4f, 108.5f, 112});
            checkSamples(asSamples(defaults.get(2)), dates, bars);
            resolutions.replaceAll((type, resolution) -> true);
            List<BandData> full = BandReaderHapi.dataRequest(resolutions, start, start + 130_000).call();
            float[] mean = new float[dates.length];
            for (int i = 0; i < mean.length; i++)
                mean[i] = i;
            checkSamples(asSamples(full.get(0)), dates, mean);
            checkSamples(asSamples(full.get(1)), dates, detail);
            checkSamples(asSamples(full.get(2)), dates, bars);
            check(dataRequests.get() == 3, "Unexpected number of data requests");
            checkBandLoading(types.get(1), mixed.get(1), start);
            holdDownloads.set(true);
            checkActiveRequestReuse(types, start, downloadStarted);
        } finally {
            releaseDownload.countDown();
            catalogs.clear();
            server.stop(0);
        }
    }

    private static void checkActiveRequestReuse(List<BandType> types, long start, CountDownLatch downloadStarted) throws Exception {
        List<Interval> intervals = List.of(new Interval(start, start + 130_000));
        Band[] bands = new Band[4];
        try {
            EventQueue.invokeAndWait(() -> {
                for (int i = 0; i < types.size(); i++) {
                    bands[i] = new Band(types.get(i));
                    BandDownloads.start(bands[i], intervals);
                }
            });
            check(downloadStarted.await(10, TimeUnit.SECONDS), "Grouped download did not start");
            EventQueue.invokeAndWait(() -> {
                try {
                    BandDownloads.stop(bands[0]);
                    bands[3] = new Band(types.get(0));
                    BandDownloads.start(bands[3], intervals);
                    Map<?, ?> pending = (Map<?, ?>) field(BandDownloads.class, null, "pendingDownloads");
                    List<?> active = (List<?>) field(BandDownloads.class, null, "activeDownloads");
                    check(pending.isEmpty() && active.size() == 1, "Matching resolution does not reuse the active request");
                    bands[3].remove();
                    bands[3] = new Band(types.get(0), true);
                    BandDownloads.start(bands[3], intervals);
                    check(pending.size() == 1 && active.size() == 1, "Different resolution reuses the old request");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        } finally {
            EventQueue.invokeAndWait(() -> {
                for (Band band : bands) {
                    if (band != null)
                        band.remove();
                }
            });
        }
    }

    private static List<BandCache.DateValue> asSamples(BandData data) {
        ArrayList<BandCache.DateValue> samples = new ArrayList<>();
        for (int i = 0; i < data.dates().length; i++)
            samples.add(new BandCache.DateValue(data.dates()[i], data.values()[i]));
        return samples;
    }

    private static void checkBandLoading(BandType type, BandData data, long start) throws Exception {
        EventQueue.invokeAndWait(() -> {
            Band full = new Band(type, true);
            Band standard = new Band(type);
            try {
                full.downloadSucceeded(data);
                BandCache cache = (BandCache) field(Band.class, full, "bandCache");
                check(cache.getValue(data.dates()[1]) == data.values()[1], "Band cache loses sub-minute samples");
                check(cache.getValue(data.dates()[3]) == YAxis.BLANK, "Band cache loses missing samples");
                check(field(Band.class, standard, "bandCache") instanceof BandCacheMinute, "Default cache changed");
                TimeAxis interval = new TimeAxis(start, start + 130_000);
                full.fetchData(interval);
                standard.fetchData(interval);
                RequestCache fullRequests = (RequestCache) field(Band.class, full, "requestCache");
                RequestCache standardRequests = (RequestCache) field(Band.class, standard, "requestCache");
                check(fullRequests.getAllRequestIntervals().equals(List.of(new Interval(start, start + 130_000))),
                        "Full-resolution requests prefetch outside the selected interval");
                long padding = 7 * TimeUtils.DAY_IN_MILLIS;
                check(standardRequests.getAllRequestIntervals().equals(List.of(new Interval(start - padding, start + 130_000 + padding))),
                        "Default prefetch changed");
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                full.remove();
                standard.remove();
            }
        });
    }

    private static Object field(Class<?> owner, Object target, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void checkEmpty() {
        BandCacheFull cache = new BandCacheFull();
        cache.addToCache(AXIS, new float[0], new long[0]);
        check(!cache.hasData(), "Empty cache reports data");
        check(cache.getValues(100, START, START + 1000).isEmpty(), "Empty cache produces a graph");
        check(cache.getValue(START) == YAxis.BLANK, "Empty cache produces a value");
        float[] bounds = cache.getBounds(START, START + 1000);
        check(bounds[0] == Float.POSITIVE_INFINITY && bounds[1] == Float.NEGATIVE_INFINITY, "Empty bounds changed");
    }

    private static void checkOrderingAndGaps() {
        BandCacheFull cache = new BandCacheFull();
        // The later interval finishes downloading first.
        cache.addToCache(AXIS, new float[]{40, 50}, new long[]{START + 400, START + 500});
        cache.addToCache(AXIS, new float[]{10, 20, YAxis.BLANK, 30},
                new long[]{START, START + 100, START + 200, START + 300});
        List<List<BandCache.DateValue>> segments = cache.getValues(1, START, START + 500);
        check(segments.size() == 2, "Missing sample does not split the graph");
        checkSamples(segments.get(0), new long[]{START, START + 100}, new float[]{10, 20});
        checkSamples(segments.get(1), new long[]{START + 300, START + 400, START + 500}, new float[]{30, 40, 50});
        check(cache.getValue(START + 200) == YAxis.BLANK, "Missing sample has a value");
        check(cache.getValue(START - 1) == YAxis.BLANK && cache.getValue(START + 501) == YAxis.BLANK,
                "Values extend beyond the cached interval");
        float[] bounds = cache.getBounds(START + 100, START + 400);
        check(bounds[0] == 20 && bounds[1] == 40, "Bounds include missing or out-of-range samples");
        segments = cache.getValues(1, START + 300, START + 400);
        check(segments.size() == 1, "Visible interval has unexpected segments");
        checkSamples(segments.getFirst(), new long[]{START + 300, START + 400}, new float[]{30, 40});

        cache.addToCache(AXIS, new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.MAX_VALUE},
                new long[]{START + 600, START + 700, START + 800});
        check(cache.getValues(100, START + 600, START + 800).isEmpty(), "Invalid values enter the graph");
    }

    private static void checkUnsortedAndEqualTimestamps() {
        BandCacheFull cache = new BandCacheFull();
        cache.addToCache(AXIS, new float[]{20, 10, 21}, new long[]{START + 200, START + 100, START + 200});
        cache.addToCache(AXIS, new float[]{22, 40, 30}, new long[]{START + 200, START + 400, START + 300});
        cache.addToCache(AXIS, new float[]{41, 50}, new long[]{START + 400, START + 500});
        cache.addToCache(AXIS, new float[0], new long[0]);
        List<List<BandCache.DateValue>> graph = cache.getValues(1, START, START + 500);
        check(graph.size() == 1, "Sorting splits continuous data");
        checkSamples(graph.getFirst(),
                new long[]{START + 100, START + 200, START + 200, START + 200, START + 300, START + 400, START + 400, START + 500},
                new float[]{10, 20, 21, 22, 30, 40, 41, 50});
        check(cache.getValue(START + 200) == 20, "Equal-timestamp ordering changes the selected value");
    }

    private static void checkFullSampleCount() {
        int count = 100_000;
        long[] dates = new long[count];
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            dates[i] = START + 100L * i;
            values[i] = i;
        }
        BandCacheFull cache = new BandCacheFull();
        cache.addToCache(AXIS, values, dates);
        List<List<BandCache.DateValue>> segments = cache.getValues(800, dates[0], dates[count - 1]);
        check(segments.size() == 1, "Continuous data was split");
        checkSamples(segments.getFirst(), dates, values);

        long next = dates[count - 1] + 100;
        cache.addToCache(AXIS, new float[]{count}, new long[]{next});
        check(cache.getValue(next) == count, "Subsequent download was discarded");
        float[] bounds = cache.getBounds(START, next);
        check(bounds[0] == 0 && bounds[1] == count, "Bounds omit samples after the old cutoff");
        check(cache.getValues(800, START, next).getFirst().size() == count + 1, "Graph omits subsequent download");

        JSONObject exported = new JSONObject();
        cache.serialize(exported, 1);
        JSONArray data = exported.getJSONArray("data");
        check(data.length() == count + 1, "Export omits cached samples");
        for (int i = 0; i <= count; i++) {
            JSONArray sample = data.getJSONArray(i);
            check(sample.getDouble(1) == i,
                    "Exported sample differs at index " + i);
        }
    }

    private static void checkSamples(List<BandCache.DateValue> actual, long[] dates, float[] values) {
        check(actual.size() == dates.length, "Expected " + dates.length + " samples, got " + actual.size());
        for (int i = 0; i < dates.length; i++) {
            BandCache.DateValue sample = actual.get(i);
            check(sample.milli == dates[i] && sample.value == values[i], "Sample differs at index " + i);
        }
    }

    private static void benchmark() throws ReflectiveOperationException {
        // Fixed synthetic 10 Hz input for comparisons across revisions. Timings are not pass/fail thresholds.
        check(instrumentation != null, "Run benchmarks through run_timeline_tests.py --benchmark");
        ThreadMXBean allocations = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(allocations.isThreadAllocatedMemorySupported(), "JVM does not support allocation measurements");
        allocations.setThreadAllocatedMemoryEnabled(true);
        int count = 1_000_000;
        long[] dates = new long[count];
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            dates[i] = START + 100L * i;
            values[i] = i % 100;
        }
        System.out.printf("Timeline benchmark: %,d samples at 10 Hz, %d warmup runs, %d measured runs (medians)%n",
                count, WARMUP_RUNS, MEASURED_RUNS);
        System.out.println("Heap allocations cover the benchmark thread. Input generation and correctness checks are excluded.");
        benchmarkLoading("Single batch", new long[][]{dates}, new float[][]{values}, List.of(0), allocations);
        int batchSize = 10_000;
        long[][] dateBatches = new long[count / batchSize][];
        float[][] valueBatches = new float[dateBatches.length][];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < dateBatches.length; i++) {
            dateBatches[i] = Arrays.copyOfRange(dates, i * batchSize, (i + 1) * batchSize);
            valueBatches[i] = Arrays.copyOfRange(values, i * batchSize, (i + 1) * batchSize);
            order.add(i);
        }
        benchmarkLoading("100 chronological batches", dateBatches, valueBatches, order, allocations);
        benchmarkLoading("100 reverse batches", dateBatches, valueBatches, order.reversed(), allocations);
        Collections.shuffle(order, new Random(1));
        BandCacheFull cache = benchmarkLoading("100 shuffled batches", dateBatches, valueBatches, order, allocations);
        System.out.printf("Cache reachable footprint: %.2f MiB (includes backing-array capacity, excludes source arrays and graph snapshots)%n",
                reachableBytes(cache) / 1048576.);
        benchmarkView("Wide view", cache, START, dates[count - 1], count, allocations);
        benchmarkView("One-minute view", cache, START + 50_000_000, START + 50_059_900, 600, allocations);
    }

    private static BandCacheFull benchmarkLoading(String name, long[][] dates, float[][] values, List<Integer> order, ThreadMXBean allocations) {
        double[] times = new double[MEASURED_RUNS];
        double[] bytes = new double[MEASURED_RUNS];
        double[] lastTimes = new double[MEASURED_RUNS];
        double[] lastBytes = new double[MEASURED_RUNS];
        BandCacheFull cache = null;
        int count = dates.length * dates[0].length;
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            long allocatedBefore = allocations.getCurrentThreadAllocatedBytes();
            long before = System.nanoTime();
            cache = new BandCacheFull();
            long lastBefore = before;
            long lastAllocated = allocatedBefore;
            for (int batch : order) {
                lastAllocated = allocations.getCurrentThreadAllocatedBytes();
                lastBefore = System.nanoTime();
                cache.addToCache(AXIS, values[batch], dates[batch]);
            }
            long finished = System.nanoTime();
            long allocatedAfter = allocations.getCurrentThreadAllocatedBytes();
            if (run >= WARMUP_RUNS) {
                int index = run - WARMUP_RUNS;
                times[index] = (finished - before) / 1e6;
                bytes[index] = (allocatedAfter - allocatedBefore) / 1048576.;
                lastTimes[index] = (finished - lastBefore) / 1e6;
                lastBytes[index] = (allocatedAfter - lastAllocated) / 1048576.;
            }
            List<List<BandCache.DateValue>> graph = cache.getValues(1200, START, START + (count - 1) * 100L);
            check(graph.size() == 1 && graph.getFirst().size() == count, "Incremental loading loses or splits samples");
            for (int i = 0; i < count; i++) {
                BandCache.DateValue sample = graph.getFirst().get(i);
                if (sample.milli != START + i * 100L || sample.value != i % 100)
                    throw new AssertionError("Incremental loading changes sample " + i);
            }
        }
        System.out.printf("%s: %.2f ms, %.2f MiB allocated; final batch %.2f ms, %.2f MiB allocated%n",
                name, median(times), median(bytes), median(lastTimes), median(lastBytes));
        return cache;
    }

    private static void benchmarkView(String name, BandCacheFull cache, long start, long end, int count, ThreadMXBean allocations)
            throws ReflectiveOperationException {
        Rectangle area = new Rectangle(0, 0, 1200, 500);
        BufferedImage image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setClip(area);
        Band band = new Band(new BandType(new JSONObject().put("name", "benchmark")), true);
        band.setDataColor(Color.WHITE);
        Method build = Band.class.getDeclaredMethod("buildPolylines", List.class, TimeAxis.Mapper.class,
                YAxis.Mapper.class, LongUnaryOperator.class, boolean.class);
        build.setAccessible(true);
        Field graphData = Band.class.getDeclaredField("graphData");
        graphData.setAccessible(true);
        TimeAxis.Mapper xMapper = new TimeAxis.Mapper(start, end, 0, area.width);
        YAxis.Mapper yMapper = AXIS.mapper(0, area.height);
        String[] phases = {"extraction", "polylines", "painting"};
        double[][] times = new double[phases.length][MEASURED_RUNS];
        double[][] bytes = new double[phases.length][MEASURED_RUNS];
        try {
            for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
                long allocatedBefore = allocations.getCurrentThreadAllocatedBytes();
                long before = System.nanoTime();
                List<List<BandCache.DateValue>> graph = cache.getValues(area.width, start, end);
                long extracted = System.nanoTime();
                long allocatedExtracted = allocations.getCurrentThreadAllocatedBytes();
                Object prepared = build.invoke(null, graph, xMapper, yMapper, LongUnaryOperator.identity(), false);
                long built = System.nanoTime();
                long allocatedBuilt = allocations.getCurrentThreadAllocatedBytes();
                check(graph.size() == 1 && graph.getFirst().size() == count, "Benchmark view loses samples");
                graphData.set(band, prepared);
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, area.width, area.height);
                long allocatedPaint = allocations.getCurrentThreadAllocatedBytes();
                long paintStart = System.nanoTime();
                band.draw(graphics, area, false);
                long painted = System.nanoTime();
                long allocatedPainted = allocations.getCurrentThreadAllocatedBytes();
                // This exact sample lies on the synthetic trace in both views.
                int sampleX = xMapper.toPixel(start + 5000);
                int sampleY = yMapper.dataToPixel(50);
                check(image.getRGB(sampleX, sampleY) == Color.WHITE.getRGB(), "Benchmark did not paint the plot");
                if (run >= WARMUP_RUNS) {
                    int index = run - WARMUP_RUNS;
                    times[0][index] = (extracted - before) / 1e6;
                    times[1][index] = (built - extracted) / 1e6;
                    times[2][index] = (painted - paintStart) / 1e6;
                    bytes[0][index] = (allocatedExtracted - allocatedBefore) / 1048576.;
                    bytes[1][index] = (allocatedBuilt - allocatedExtracted) / 1048576.;
                    bytes[2][index] = (allocatedPainted - allocatedPaint) / 1048576.;
                }
            }
            System.out.printf("%s: %,d samples, %dx%d single-color offscreen plot%n", name, count, area.width, area.height);
            for (int phase = 0; phase < phases.length; phase++)
                System.out.printf("  %s: %.3f ms, %.3f MiB allocated%n", phases[phase], median(times[phase]), median(bytes[phase]));
        } finally {
            graphics.dispose();
            band.remove();
        }
    }

    private static double median(double[] values) {
        Arrays.sort(values);
        return values[values.length / 2];
    }

    private static long reachableBytes(Object root) throws IllegalAccessException {
        // Reachable object sizes, not a whole-heap delta or a claim about exclusive GC retention.
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        Map<Class<?>, List<Field>> references = new HashMap<>();
        ArrayDeque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        long bytes = 0;
        while (!pending.isEmpty()) {
            Object object = pending.removeLast();
            if (seen.put(object, true) != null)
                continue;
            bytes += instrumentation.getObjectSize(object);
            Class<?> type = object.getClass();
            if (type.isArray()) {
                if (!type.getComponentType().isPrimitive()) {
                    for (Object value : (Object[]) object) {
                        if (value != null)
                            pending.add(value);
                    }
                }
                continue;
            }
            List<Field> fields = references.get(type);
            if (fields == null) {
                fields = new ArrayList<>();
                for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
                    for (Field field : owner.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                            field.setAccessible(true);
                            fields.add(field);
                        }
                    }
                }
                references.put(type, fields);
            }
            for (Field field : fields) {
                Object value = field.get(object);
                if (value != null)
                    pending.add(value);
            }
        }
        return bytes;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
