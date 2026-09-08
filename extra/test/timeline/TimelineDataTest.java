package org.helioviewer.jhv.timelines.band;

import java.util.List;

import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

public final class TimelineDataTest {

    private static final long START = 1_700_000_000_123L;
    private static final YAxis AXIS = new YAxis(0, 100, YAxis.generateScale("linear", ""));

    public static void main(String[] args) {
        checkEmpty();
        checkOrderingAndGaps();
        checkFullSampleCount();
        System.out.println("Timeline data tests passed");
        if (args.length > 0 && "--benchmark".equals(args[0]))
            benchmark();
    }

    private static void checkEmpty() {
        BandCacheAll cache = new BandCacheAll();
        cache.addToCache(AXIS, new float[0], new long[0]);
        check(!cache.hasData(), "Empty cache reports data");
        check(cache.getValues(100, START, START + 1000).isEmpty(), "Empty cache produces a graph");
        check(cache.getValue(START) == YAxis.BLANK, "Empty cache produces a value");
        float[] bounds = cache.getBounds(START, START + 1000);
        check(bounds[0] == Float.POSITIVE_INFINITY && bounds[1] == Float.NEGATIVE_INFINITY, "Empty bounds changed");
    }

    private static void checkOrderingAndGaps() {
        BandCacheAll cache = new BandCacheAll();
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

    private static void checkFullSampleCount() {
        int count = 100_000;
        long[] dates = new long[count];
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            dates[i] = START + 100L * i;
            values[i] = i;
        }
        BandCacheAll cache = new BandCacheAll();
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

    private static void benchmark() {
        // Fixed synthetic 10 Hz input for comparisons across revisions. Timings are not pass/fail thresholds.
        int count = 1_000_000;
        long[] dates = new long[count];
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            dates[i] = START + 100L * i;
            values[i] = i % 100;
        }
        System.out.println("Cache benchmark: 1,000,000 samples at 10 Hz, 3 warmup runs, 5 measured runs");
        for (int run = 0; run < 8; run++) {
            BandCacheAll cache = new BandCacheAll();
            long before = System.nanoTime();
            cache.addToCache(AXIS, values, dates);
            long inserted = System.nanoTime();
            List<List<BandCache.DateValue>> graph = cache.getValues(1200, START, dates[count - 1]);
            long extracted = System.nanoTime();
            check(graph.size() == 1, "Benchmark data was split");
            checkSamples(graph.getFirst(), dates, values);
            if (run >= 3)
                System.out.printf("Run %d: insertion %.1f ms, graph extraction %.1f ms%n",
                        run - 2, (inserted - before) / 1e6, (extracted - inserted) / 1e6);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
