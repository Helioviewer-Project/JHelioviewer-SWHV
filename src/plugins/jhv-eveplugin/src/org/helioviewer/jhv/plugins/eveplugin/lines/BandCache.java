package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

class BandCache {

    private static final long DAYS_PER_CHUNK = 8;
    private static final long MILLIS_PER_TICK = 60000;
    private static final long CHUNKED_SIZE = TimeUtils.DAY_IN_MILLIS / MILLIS_PER_TICK * DAYS_PER_CHUNK;
    private static final long MILLIS_PER_CHUNK = TimeUtils.DAY_IN_MILLIS * DAYS_PER_CHUNK;
    private static final int MAX_LEVEL = 12;
    private static final int FACTOR_STEP = 2;
    private boolean hasData = false;

    private final HashMap<Long, DataChunk> cacheMap = new HashMap<>();

    private static long date2key(long date) {
        return date / MILLIS_PER_CHUNK;
    }

    public boolean hasData() {
        return hasData;
    }

    void addToCache(float[] values, long[] dates) {
        if (values.length != 0) {
            hasData = true;
        }
        for (int i = 0; i < values.length; i++) {
            long key = date2key(dates[i]);
            DataChunk cache = cacheMap.get(key);
            if (cache == null) {
                cache = new DataChunk(key);
                cacheMap.put(key, cache);
            }
            if (values[i] > DrawConstants.DISCARD_LEVEL_LOW && values[i] < DrawConstants.DISCARD_LEVEL_HIGH) {
                cache.setValue((int) ((dates[i] % (MILLIS_PER_CHUNK)) / MILLIS_PER_TICK), values[i]);
            }
        }
    }

    public float[] getBounds(TimeAxis timeAxis) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        long keyEnd = date2key(timeAxis.end);
        long key = date2key(timeAxis.start);
        while (key <= keyEnd) {
            DataChunk cache = cacheMap.get(key);
            key++;
            if (cache == null)
                continue;
            float[] values = cache.getValues(0);
            long[] dates = cache.getDates(0);

            for (int i = 0; i < values.length; i++) {
                float value = values[i];
                if (value != Float.MIN_VALUE && timeAxis.start <= dates[i] && dates[i] <= timeAxis.end) {
                    min = Math.min(value, min);
                    max = Math.max(value, max);
                }
            }
        }
        return new float[] { min, max };
    }

    void createPolyLines(Rectangle graphArea, TimeAxis timeAxis, YAxis yAxis, ArrayList<GraphPolyline> graphPolylines) {
        long keyEnd = date2key(timeAxis.end);
        long key = date2key(timeAxis.start);
        int level = 0;
        double factor = 1;
        double elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
        double noelements = (timeAxis.end - timeAxis.start) / elsz;

        double graphWidth = graphArea.width * GLInfo.pixelScaleFloat[0];
        while (level < MAX_LEVEL - 1 && noelements > graphWidth) {
            level++;
            factor *= FACTOR_STEP;
            elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
            noelements = (timeAxis.end - timeAxis.start) / elsz;
        }
        ArrayList<Integer> tvalues = new ArrayList<>();
        ArrayList<Integer> tdates = new ArrayList<>();
        while (key <= keyEnd) {
            DataChunk cache = cacheMap.get(key);
            key++;
            if (cache == null)
                continue;
            float[] values = cache.getValues(level);
            long[] dates = cache.getDates(level);
            int i = 0;
            while (i < values.length) {
                float value = values[i];
                if (value <= Float.MIN_VALUE && !tvalues.isEmpty()) {
                    graphPolylines.add(new GraphPolyline(tdates, tvalues));
                    tvalues.clear();
                    tdates.clear();
                } else if (value > Float.MIN_VALUE) {
                    tdates.add(timeAxis.value2pixel(graphArea.x, graphArea.width, dates[i]));
                    tvalues.add(yAxis.value2pixel(graphArea.y, graphArea.height, value));
                }
                i++;
            }
        }
        if (!tvalues.isEmpty()) {
            graphPolylines.add(new GraphPolyline(tdates, tvalues));
        }
    }

    public float getValue(long ts) {
        long key = date2key(ts);
        DataChunk cache = cacheMap.get(key);
        if (cache != null) {
            long[] dates = cache.getDates(0);
            int len = dates.length;
            int idx = (int) ((len - 1) * 1. * (ts - dates[0]) / (dates[len - 1] - dates[0]) + 0.5);

            if (idx >= 0 && idx < len) {
                return cache.getValues(0)[idx];
            }
        }
        return Float.MIN_VALUE;
    }

    private static class DataChunk {
        private final float[][] values = new float[MAX_LEVEL][];
        private final long[][] dates = new long[MAX_LEVEL][];

        private DataChunk(long key) {
            int factor = 1;
            for (int i = 0; i < MAX_LEVEL; i++) {
                values[i] = new float[(int) CHUNKED_SIZE / factor];
                Arrays.fill(values[i], Float.MIN_VALUE);
                dates[i] = new long[(int) CHUNKED_SIZE / factor];
                factor *= FACTOR_STEP;
            }

            long startdate = key * MILLIS_PER_CHUNK;
            factor = 1;
            for (int j = 0; j < values.length; j++) {
                for (int i = 0; i < values[j].length; i++) {
                    dates[j][i] = startdate + i * MILLIS_PER_TICK * factor;
                }
                factor *= FACTOR_STEP;
            }
        }

        private void setValue(int minuteOfDay, float value) {
            int factor = 1;
            for (int i = 0; i < values.length; i++) {
                int idx = minuteOfDay / factor;
                if (idx >= values[i].length) {
                    idx = values[i].length - 1;
                }
                values[i][idx] = Math.max(values[i][idx], value);
                factor *= FACTOR_STEP;
            }

        }

        private float[] getValues(int level) {
            return values[level];
        }

        private long[] getDates(int level) {
            return dates[level];
        }
    }

    static class GraphPolyline {

        public final int[] xPoints;
        public final int[] yPoints;

        private GraphPolyline(List<Integer> dates, List<Integer> values) {
            int llen = dates.size();
            xPoints = new int[llen];
            yPoints = new int[llen];
            for (int j = 0; j < llen; j++) {
                xPoints[j] = dates.get(j);
                yPoints[j] = values.get(j);
            }
        }
    }

}
