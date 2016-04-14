package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.HashMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.TimeUtils;

public class EVECache {

    private final HashMap<Long, EVEDataOfChunk> cacheMap = new HashMap<Long, EVEDataOfChunk>();
    private static final double DISCARD_LOG_LEVEL_LOW = 1e-10;
    private static final double DISCARD_LOG_LEVEL_HIGH = 1e+4;
    private static final int DAYS_PER_CHUNK = 10;
    static final int MILLIS_PER_TICK = 60000;
    static final int CHUNKED_SIZE = TimeUtils.DAY_IN_MILLIS / MILLIS_PER_TICK * DAYS_PER_CHUNK;
    static final long MILLIS_PER_CHUNK = TimeUtils.DAY_IN_MILLIS * DAYS_PER_CHUNK;

    private static long date2key(long date) {
        return date / MILLIS_PER_CHUNK;
    }

    public void add(final float[] values, final long[] dates) {

        for (int i = 0; i < values.length; i++) {
            long key = date2key(dates[i]);

            EVEDataOfChunk cache = cacheMap.get(key);
            if (cache == null) {
                cache = new EVEDataOfChunk(key);
                cacheMap.put(key, cache);
            }
            if (values[i] > DISCARD_LOG_LEVEL_LOW && values[i] < DISCARD_LOG_LEVEL_HIGH) {
                cache.setValue((int) ((dates[i] % (MILLIS_PER_CHUNK)) / MILLIS_PER_TICK), values[i], dates[i]);
            }
        }
    }

    public EVEValues getValuesInInterval(final Interval interval, Rectangle space) {
        long intervalStart = interval.start;
        long intervalEnd = interval.end;
        long intervalWidth = intervalEnd - intervalStart;
        int spaceWidth = space.width;
        long binStart;
        long binEnd;

        int numberOfBins;
        long timePerBin;
        if (space.width < intervalWidth / MILLIS_PER_TICK) {
            binStart = intervalStart - (intervalWidth / spaceWidth / 2);
            binEnd = intervalEnd + (intervalWidth / spaceWidth / 2);
            numberOfBins = spaceWidth + 1;
            timePerBin = intervalWidth / spaceWidth;
        } else {
            numberOfBins = (int) (intervalWidth / MILLIS_PER_TICK) + 1;
            timePerBin = intervalWidth / numberOfBins;
            binStart = intervalStart - timePerBin / 2;
            binEnd = intervalEnd + timePerBin / 2;
        }

        final EVEValues result = new EVEValues(binStart, binEnd, intervalStart, numberOfBins, timePerBin);

        long keyEnd = date2key(binEnd);
        long key = date2key(binStart);

        while (key <= keyEnd) {
            EVEDataOfChunk cache = cacheMap.get(key);
            if (cache != null) {
                cache.fillResult(result);
            }

            key++;
        }

        return result;
    }

    public boolean hasDataInInterval(Interval selectedInterval) {
        long keyEnd = date2key(selectedInterval.end);
        long key = date2key(selectedInterval.start);

        while (key <= keyEnd) {
            EVEDataOfChunk cache = cacheMap.get(key);

            if (cache != null && cache.hasData()) {
                return true;
            }
            key++;
        }
        return false;
    }

}
