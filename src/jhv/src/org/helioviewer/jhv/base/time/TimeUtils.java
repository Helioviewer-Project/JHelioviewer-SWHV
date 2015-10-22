package org.helioviewer.jhv.base.time;

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

    public static final ImmutableDateTime epoch = ImmutableDateTime.parseDateTime("2000-01-01T00:00:00");

    private static final TimeUtils instance = new TimeUtils();

    public static TimeUtils getSingletonInstance() {
        return instance;
    }

    private TimeUtils() {
    }

}
