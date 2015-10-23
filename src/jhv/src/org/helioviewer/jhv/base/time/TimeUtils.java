package org.helioviewer.jhv.base.time;

import java.util.Calendar;
import java.util.Date;

import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

public class TimeUtils {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final FastDateFormat utcDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss", UTC);
    public static final FastDateFormat sqlDateFormat = FastDateFormat.getInstance(SQL_DATE_FORMAT, UTC);
    public static final FastDateFormat utcFullDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS", UTC);
    public static final FastDateFormat apiDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final FastDateFormat filenameDateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH.mm.ss");
    public static final FastDateFormat timeDateFormat = FastDateFormat.getInstance("HH:mm:ss");

    public static final JHVDate epoch = JHVDate.parseDateTime("2000-01-01T00:00:00");

    private static final TimeUtils instance = new TimeUtils();

    public static TimeUtils getSingletonInstance() {
        return instance;
    }

    private TimeUtils() {
    }

    /**
     * Gets the date of give date with hour, minute, seconds, milliseconds to 0.
     *
     * @param date
     *            the date to round
     * @return the rounded date
     */
    public static Date getCurrentDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Gets the date rounded up to the following day. So day+1, hour, minute,
     * second, millisecond 0.
     *
     * @param date
     *            The date to round up.
     * @return The rounded up date on the day
     */
    public static Date getNextDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

}
