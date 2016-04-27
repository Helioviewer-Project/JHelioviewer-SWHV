package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.util.Arrays;

public class EVEDataOfChunk {

    private final float[] values = new float[(int) Band.CHUNKED_SIZE];
    private final long[] dates = new long[(int) Band.CHUNKED_SIZE];
    private boolean hasData = false;

    public EVEDataOfChunk(long key) {
        Arrays.fill(values, Float.NaN);
        long startdate = key * Band.MILLIS_PER_CHUNK;
        for (int i = 0; i < values.length; i++) {
            dates[i] = startdate + i * Band.MILLIS_PER_TICK;
        }
    }

    public void setValue(int minuteOfDay, float value, long date) {
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
