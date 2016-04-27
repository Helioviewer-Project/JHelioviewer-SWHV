package org.helioviewer.jhv.base.time;

import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;
import org.helioviewer.jhv.base.interval.Interval;

public class TimeUtils {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MINUTE_IN_MILLIS = 60000;

    public static final FastDateFormat utcDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss", UTC);
    public static final FastDateFormat sqlDateFormat = FastDateFormat.getInstance(SQL_DATE_FORMAT, UTC);
    public static final FastDateFormat utcFullDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS", UTC);
    public static final FastDateFormat apiDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final FastDateFormat filenameDateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH.mm.ss");
    public static final FastDateFormat timeDateFormat = FastDateFormat.getInstance("HH:mm:ss");
    public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");

    public static final JHVDate Epoch = JHVDate.parseDateTime("2000-01-01T00:00:00");

    private static final TimeUtils instance = new TimeUtils();

    public static TimeUtils getSingletonInstance() {
        return instance;
    }

    private TimeUtils() {
    }

    public static Interval makeCompleteDay(final long start, final long end) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (end > now) {
            endDate = now;
        }

        long new_start = start - start % TimeUtils.DAY_IN_MILLIS;
        long new_end = endDate - endDate % TimeUtils.DAY_IN_MILLIS + TimeUtils.DAY_IN_MILLIS;

        return new Interval(new_start, new_end);
    }

}
