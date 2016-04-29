package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

public class BandCache {

    private static final double DISCARD_LOG_LEVEL_LOW = 1e-10;
    private static final double DISCARD_LOG_LEVEL_HIGH = 1e+4;
    private static long DAYS_PER_CHUNK = 8;
    private static final long MILLIS_PER_TICK = 60000;
    private static final long CHUNKED_SIZE = TimeUtils.DAY_IN_MILLIS / MILLIS_PER_TICK * DAYS_PER_CHUNK;
    private static final long MILLIS_PER_CHUNK = TimeUtils.DAY_IN_MILLIS * DAYS_PER_CHUNK;
    private static int MAX_LEVEL = 12;
    private static int FACTOR_STEP = 2;

    private final HashMap<Long, DataChunk> cacheMap = new HashMap<Long, DataChunk>();

    private static long date2key(long date) {
        return date / MILLIS_PER_CHUNK;
    }

    void addToCache(final float[] values, final long[] dates) {
        for (int i = 0; i < values.length; i++) {
            long key = date2key(dates[i]);
            DataChunk cache = cacheMap.get(key);
            if (cache == null) {
                cache = new DataChunk(key);
                cacheMap.put(key, cache);
            }
            if (values[i] > DISCARD_LOG_LEVEL_LOW && values[i] < DISCARD_LOG_LEVEL_HIGH) {
                cache.setValue((int) ((dates[i] % (MILLIS_PER_CHUNK)) / MILLIS_PER_TICK), values[i]);
            }
        }

    }

    void createPolyLines(TimeAxis timeAxis, YAxis yAxis, ArrayList<GraphPolyline> graphPolylines) {
        long keyEnd = date2key(timeAxis.end);
        long key = date2key(timeAxis.start);
        int level = 0;
        double factor = 1;
        double elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
        double noelements = (timeAxis.end - timeAxis.start) / (elsz);
        double graphWidth = EVEPlugin.dc.getGraphSize().width * GLInfo.pixelScaleFloat[0];
        while (level < MAX_LEVEL - 1 && noelements > graphWidth) {
            level++;
            factor *= FACTOR_STEP;
            elsz = 1. * MILLIS_PER_CHUNK / CHUNKED_SIZE * factor;
            noelements = (timeAxis.end - timeAxis.start) / (elsz);
        }
        Rectangle graphArea = EVEPlugin.dc.getGraphArea();
        ArrayList<Integer> tvalues = new ArrayList<Integer>();
        ArrayList<Integer> tdates = new ArrayList<Integer>();
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

    private static class DataChunk {
        private final float[][] values = new float[MAX_LEVEL][];
        private final long[][] dates = new long[MAX_LEVEL][];

        public DataChunk(long key) {
            int factor = 1;
            for (int i = 0; i < MAX_LEVEL; i++) {
                values[i] = new float[(int) CHUNKED_SIZE / factor];
                dates[i] = new long[(int) CHUNKED_SIZE / factor];
                factor = factor * FACTOR_STEP;
            }
            for (int j = 0; j < values.length; j++) {
                Arrays.fill(values[j], Float.MIN_VALUE);
            }
            long startdate = key * MILLIS_PER_CHUNK;
            factor = 1;
            for (int j = 0; j < values.length; j++) {
                for (int i = 0; i < values[j].length; i++) {
                    dates[j][i] = startdate + i * MILLIS_PER_TICK * factor;
                }
                factor = factor * FACTOR_STEP;
            }
        }

        public void setValue(int minuteOfDay, float value) {
            int factor = 1;
            for (int i = 0; i < values.length; i++) {
                int idx = minuteOfDay / factor;
                if (idx >= values[i].length) {
                    idx = values[i].length - 1;
                }
                values[i][idx] = Math.max(values[i][idx], value);
                factor = factor * FACTOR_STEP;
            }

        }

        public float[] getValues(int level) {
            return values[level];
        }

        public long[] getDates(int level) {
            return dates[level];
        }
    }

    static class GraphPolyline {

        public final int[] xPoints;
        public final int[] yPoints;

        public GraphPolyline(List<Integer> dates, List<Integer> values) {
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
