package org.helioviewer.jhv.base.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    private static final ZoneOffset ZERO = ZoneOffset.ofTotalSeconds(0);
    private static final DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MINUTE_IN_MILLIS = 60000;

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

    public static String formatDate(long milli) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(milli).atOffset(ZERO));
    }

    public static String formatTime(long milli) {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(Instant.ofEpochMilli(milli).atOffset(ZERO));
    }

    public static String formatFilename(long milli) {
        return fileFormatter.format(Instant.ofEpochMilli(milli).atOffset(ZERO));
    }

    public static long parse(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZERO).toEpochMilli();
    }

    public static long parseDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay() * DAY_IN_MILLIS;
    }

    public static long parseTime(String date) {
        return LocalTime.parse(date, DateTimeFormatter.ISO_LOCAL_TIME).toSecondOfDay() * 1000L;
    }

}
