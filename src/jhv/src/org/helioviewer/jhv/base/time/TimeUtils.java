package org.helioviewer.jhv.base.time;

import java.time.ZoneOffset;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;
import org.helioviewer.jhv.base.interval.Interval;

public class TimeUtils {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final ZoneOffset ZERO = ZoneOffset.ofTotalSeconds(0);

    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MINUTE_IN_MILLIS = 60000;

    public static final FastDateFormat utcDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss", UTC);
    public static final FastDateFormat sqlDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", UTC);
    public static final FastDateFormat utcFullDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS", UTC);
    public static final FastDateFormat apiDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", UTC);
    public static final FastDateFormat filenameDateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH.mm.ss");
    public static final FastDateFormat timeDateFormat = FastDateFormat.getInstance("HH:mm:ss");
    public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");

    public static final JHVDate EPOCH = new JHVDate("2000-01-01T00:00:00");
    public static final JHVDate MINIMAL_DATE = new JHVDate("1970-01-01T00:00:00");
    public static final JHVDate MAXIMAL_DATE = new JHVDate("2050-01-01T00:00:00");

    public static Interval makeCompleteDay(long start, long end) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (end > now) {
            endDate = now;
        }

        long new_start = start - start % DAY_IN_MILLIS;
        long new_end = endDate - endDate % DAY_IN_MILLIS + DAY_IN_MILLIS;

        return new Interval(new_start, new_end);
    }

}
