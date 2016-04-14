package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.util.Arrays;

public class EVEDataOfDay {

    private final float[] values = new float[EVECache.CHUNKED_SIZE];
    private final long[] dates = new long[EVECache.CHUNKED_SIZE];
    private boolean hasData = false;

    public EVEDataOfDay(final long key) {
        Arrays.fill(values, Float.NaN);
        long startdate = key * EVECache.MILLIS_PER_CHUNK;
        for (int i = 0; i < values.length; i++) {
            dates[i] = startdate + i * EVECache.MILLIS_PER_TICK;
        }
    }

    public void setValue(final int minuteOfDay, final float value, final long date) {
        values[minuteOfDay] = value;
        hasData = true;
    }

    public void fillResult(EVEValues result) {
        result.addValues(dates, values);
    }

    public boolean hasData() {
        return hasData;
    }

}