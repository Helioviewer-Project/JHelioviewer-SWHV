package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

class BandCacheMinute implements BandCache {

    private static final long DAYS_PER_CHUNK = 8;
    private static final long MILLIS_PER_TICK = 60000;
    private static final long CHUNKED_SIZE = TimeUtils.DAY_IN_MILLIS / MILLIS_PER_TICK * DAYS_PER_CHUNK;
    private static final long MILLIS_PER_CHUNK = TimeUtils.DAY_IN_MILLIS * DAYS_PER_CHUNK;
    private static final int MAX_LEVEL = 12;
    private static final int FACTOR_STEP = 2;

    private boolean hasData;

    private final HashMap<Long, DataChunk> cacheMap = new HashMap<>();

    private static long date2key(long date) {
        return date / MILLIS_PER_CHUNK;
    }

    @Override
    public boolean hasData() {
        return hasData;
    }

    @Override
    public void addToCache(YAxis yAxis, float[] values, long[] dates) {
        int len = values.length;
        if (len > 0) {
            hasData = true;
        }

        boolean max = yAxis.preferMax();
        for (int i = 0; i < len; i++) {
            long key = date2key(dates[i]);
            DataChunk cache = cacheMap.computeIfAbsent(key, DataChunk::new);
            cache.setValue(max, (int) ((dates[i] % MILLIS_PER_CHUNK) / MILLIS_PER_TICK), yAxis.clip(values[i]));
        }
    }

    @Override
    public float[] getBounds(long start, long end) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        long key = date2key(start);
        long keyEnd = date2key(end);
        while (key <= keyEnd) {
            DataChunk cache = cacheMap.get(key);
            key++;
            if (cache == null) {
                continue;
            }
            float[] values = cache.getValues(0);

            for (int i = 0; i < values.length; i++) {
                float value = values[i];
                long date = cache.getDate(0, i);
                if (value != YAxis.BLANK && start <= date && date <= end) {
                    min = Math.min(value, min);
                    max = Math.max(value, max);
                }
            }
        }
        return new float[]{min, max};
    }

    @Override
    public List<List<DateValue>> getValues(double graphWidth, long start, long end) {
        int level = 0;
        double factor = 1;
        double elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
        long aWidth = end - start;
        double numElements = aWidth / elsz;

        while (level < MAX_LEVEL - 1 && numElements > graphWidth) {
            level++;
            factor *= FACTOR_STEP;
            elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
            numElements = aWidth / elsz;
        }

        List<List<DateValue>> ret = new ArrayList<>();
        List<DateValue> list = new ArrayList<>();

        long key = date2key(start);
        long keyEnd = date2key(end);
        while (key <= keyEnd) {
            DataChunk cache = cacheMap.get(key);
            key++;
            if (cache == null) {
                continue;
            }
            float[] values = cache.getValues(level);
            int i = 0;
            while (i < values.length) {
                float value = values[i];
                long date = cache.getDate(level, i);
                if (date < start || date > end || value == YAxis.BLANK) {
                    ret.add(list);
                    list = new ArrayList<>();
                } else {
                    list.add(new DateValue(date, value));
                }
                i++;
            }
        }
        ret.add(list);
        return ret;
    }

    @Override
    public float getValue(long ts) {
        long key = date2key(ts);
        DataChunk cache = cacheMap.get(key);
        if (cache != null) {
            float[] values = cache.getValues(0);
            int idx = (int) ((ts - cache.startDate) / (double) MILLIS_PER_TICK + 0.5);
            if (idx >= 0 && idx < values.length)
                return values[idx];
        }
        return YAxis.BLANK;
    }

    @Override
    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        cacheMap.values().forEach(chunk -> chunk.serialize(ja, f));
        jo.put("data", ja);
    }

    private static class DataChunk {

        private final float[][] values = new float[MAX_LEVEL][];
        private final long startDate;

        DataChunk(long key) {
            startDate = key * MILLIS_PER_CHUNK;
            int factor = 1;
            for (int i = 0; i < MAX_LEVEL; i++) {
                values[i] = new float[(int) CHUNKED_SIZE / factor];
                Arrays.fill(values[i], YAxis.BLANK);
                factor *= FACTOR_STEP;
            }
        }

        void setValue(boolean max, int minuteOfDay, float value) {
            int factor = 1;
            for (int i = 0; i < values.length; i++) {
                //if (values[i].length == 0) -- if ever MILLIS_PER_TICK > 5min
                //    continue;
                int idx = minuteOfDay / factor;
                if (idx >= values[i].length) {
                    idx = values[i].length - 1;
                }
                values[i][idx] = max ? Math.max(values[i][idx], value) : value;
                factor *= FACTOR_STEP;
            }

        }

        float[] getValues(int level) {
            return values[level];
        }

        long getDate(int level, int index) {
            return startDate + index * MILLIS_PER_TICK * (1L << level);
        }

        void serialize(JSONArray ja, double f) {
            float[] v = values[0];
            for (int i = 0; i < v.length; i++)
                ja.put(new JSONArray().put(getDate(0, i) / 1000L).put(f * v[i]));
        }

    }

}
