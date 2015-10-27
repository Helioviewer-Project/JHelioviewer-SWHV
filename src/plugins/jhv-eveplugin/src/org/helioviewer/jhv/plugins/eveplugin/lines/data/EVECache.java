package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.helioviewer.jhv.base.interval.Interval;

/**
 *
 * @author Stephan Pagel
 * */
public class EVECache {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final HashMap<Integer, EVEDataOfDay> cacheMap = new HashMap<Integer, EVEDataOfDay>();
    private final boolean logCache;
    private static double DISCARD_LOG_LEVEL = 10e-50;

    public EVECache(boolean logCache) {
        this.logCache = logCache;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void add(final float[] values, final long[] dates) {
        GregorianCalendar calendar = new GregorianCalendar();

        for (int i = 0; i < values.length; i++) {
            calendar.setTimeInMillis(dates[i]);
            final Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));

            EVEDataOfDay cache = cacheMap.get(key);
            if (cache == null) {
                cache = new EVEDataOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                cacheMap.put(key, cache);
            }
            if (!logCache || values[i] > DISCARD_LOG_LEVEL) {
                cache.setValue(calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE), values[i], dates[i]);
            }
        }
    }

    public EVEValues getValuesInInterval(final Interval<Date> interval, Rectangle space) {
        long intervalWidth = interval.getEnd().getTime() - interval.getStart().getTime();
        int spaceWidth = space.width;
        long binStart;
        long binEnd;
        long intervalStart = interval.getStart().getTime();
        long intervalEnd = interval.getEnd().getTime();
        int numberOfBins;
        long timePerBin;
        if (space.width < (intervalWidth / 60000)) {
            binStart = intervalStart - (intervalWidth / spaceWidth / 2);
            binEnd = intervalEnd + (intervalWidth / spaceWidth / 2);
            numberOfBins = spaceWidth + 1;
            timePerBin = intervalWidth / spaceWidth;
        } else {
            numberOfBins = (int) intervalWidth / 60000 + 1;
            timePerBin = intervalWidth / numberOfBins;
            binStart = intervalStart - timePerBin / 2;
            binEnd = intervalEnd + timePerBin / 2;

        }

        final EVEValues result = new EVEValues(binStart, binEnd, intervalStart, numberOfBins, timePerBin);

        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTimeInMillis(binEnd);
        Integer keyEnd = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        calendar.setTimeInMillis(binStart);
        Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));

        while (key <= keyEnd) {
            EVEDataOfDay cache = cacheMap.get(key);
            if (cache == null) {
                cache = new EVEDataOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }

            // final Pair<long[], double[]> pair =
            // cache.getValuesInInterval(interval);
            // result.addValues(pair.a, pair.b);
            cache.fillResult(result);

            calendar.add(Calendar.DAY_OF_YEAR, 1);
            key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        }

        return result;
    }

    public boolean hasDataInInterval(Interval<Date> selectedInterval) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(selectedInterval.getEnd());
        Integer keyEnd = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        calendar.setTime(selectedInterval.getStart());
        Integer key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        while (key <= keyEnd) {
            EVEDataOfDay cache = cacheMap.get(key);
            if (cache != null && cache.hasData()) {
                return true;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            key = new Integer(calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR));
        }
        return false;
    }

}
