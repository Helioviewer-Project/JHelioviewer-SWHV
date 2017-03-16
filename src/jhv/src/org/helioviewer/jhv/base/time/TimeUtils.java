package org.helioviewer.jhv.base.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

public class TimeUtils {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final ZoneOffset ZERO = ZoneOffset.ofTotalSeconds(0);
    private static final DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MINUTE_IN_MILLIS = 60000;

    public static final FastDateFormat filenameDateFormat = FastDateFormat.getInstance("yyyy-MM-dd_HH.mm.ss");
    public static final FastDateFormat timeDateFormat = FastDateFormat.getInstance("HH:mm:ss");
    public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd");

    public static final JHVDate EPOCH = new JHVDate("2000-01-01T00:00:00");
    public static final JHVDate MINIMAL_DATE = new JHVDate("1970-01-01T00:00:00");
    public static final JHVDate MAXIMAL_DATE = new JHVDate("2050-01-01T00:00:00");

    public static String format(long milli) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochMilli(milli).atOffset(ZERO));
    }

    public static String formatZ(long milli) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(milli));
    }

    public static String formatSQL(long milli) {
        return sqlFormatter.format(Instant.ofEpochMilli(milli).atOffset(ZERO));
    }

    public static long parse(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZERO).toEpochMilli();
    }

    public static long parseSQL(String date) {
        return LocalDateTime.parse(date, sqlFormatter).toInstant(ZERO).toEpochMilli();
    }

}
