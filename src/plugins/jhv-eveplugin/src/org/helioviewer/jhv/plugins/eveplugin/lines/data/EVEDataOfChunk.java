package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.util.Arrays;

public class EVEDataOfChunk {
    public static int MAX_LEVEL = 12;
    public static int FACTOR_STEP = 2;
    private final float[][] values = new float[MAX_LEVEL][];
    private final long[][] dates = new long[MAX_LEVEL][];

    public EVEDataOfChunk(long key) {
        int factor = 1;
        for (int i = 0; i < MAX_LEVEL; i++) {
            values[i] = new float[(int) Band.CHUNKED_SIZE / factor];
            dates[i] = new long[(int) Band.CHUNKED_SIZE / factor];
            factor = factor * FACTOR_STEP;
        }
        for (int j = 0; j < values.length; j++) {
            Arrays.fill(values[j], Float.MIN_VALUE);
        }
        long startdate = key * Band.MILLIS_PER_CHUNK;
        factor = 1;
        for (int j = 0; j < values.length; j++) {
            for (int i = 0; i < values[j].length; i++) {
                dates[j][i] = startdate + i * Band.MILLIS_PER_TICK * factor;
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
