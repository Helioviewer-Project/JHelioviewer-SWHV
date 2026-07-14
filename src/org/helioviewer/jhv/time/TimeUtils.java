package org.helioviewer.jhv.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.helioviewer.jhv.astronomy.Spice;

import org.everit.json.schema.FormatValidator;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

public class TimeUtils {

    private static final DateTimeFormatter milliFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final DateTimeFormatter sqlTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId zoneId = ZoneId.of(System.getProperty("user.timezone"));
    private static final DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").withZone(zoneId); //! local time
    private static final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(zoneId); //! local time

    private static final PrettyTimeParser prettyParser = new PrettyTimeParser();

    public static final long DAY_IN_MILLIS = 86400000;
    public static final long MINUTE_IN_MILLIS = 60000;

    public static final JHVTime START = new JHVTime(floorSec(System.currentTimeMillis()));
    public static final JHVTime MINIMAL_TIME = new JHVTime("1990-01-01T00:00:00");
    public static final JHVTime MAXIMAL_TIME = new JHVTime("2050-01-01T00:00:00");
    public static final JHVTime J2000 = new JHVTime("2000-01-01T12:00:00");

    private static final double MAX_FRAMES = 96;
    private static final int MIN_DEF_CADENCE = 60;

    public static int defaultCadence(long start, long end) {
        return (int) Math.max(MIN_DEF_CADENCE, (end - start) / MAX_FRAMES / 1000);
    }

    // A duration rendered to 3 significant figures in its largest sensible unit, e.g. "2.03 d".
    public static String formatDurationSig(long millis) {
        double sec = Math.max(0, millis) / 1000.;
        if (sec >= 86400)
            return sig3(sec / 86400) + " d";
        if (sec >= 3600)
            return sig3(sec / 3600) + " h";
        if (sec >= 60)
            return sig3(sec / 60) + " min";
        return sig3(sec) + " s";
    }

    private static String sig3(double v) {
        if (v <= 0)
            return "0";
        int decimals = Math.max(0, 2 - (int) Math.floor(Math.log10(v)));
        return String.format("%." + decimals + "f", v);
    }

    public static long floorSec(long milli) {
        return (milli / 1000L) * 1000L;
    }

    public static long roundSec(long milli) {
        return ((milli + 500L) / 1000L) * 1000L;
    }

    public static long ceilSec(long milli) {
        return ((milli + 999L) / 1000L) * 1000L;
    }

    public static long floorDay(long milli) {
        return milli - milli % DAY_IN_MILLIS;
    }

    public static String format(DateTimeFormatter formatter, long milli) {
        return formatter.format(Instant.ofEpochMilli(milli).atOffset(ZoneOffset.UTC));
    }

    public static String format(long milli) { // always three digits milli
        return format(milliFormatter, milli);
    }

    public static String formatShort(long milli) { // without trailing zeros milli
        return format(DateTimeFormatter.ISO_LOCAL_DATE_TIME, milli);
    }

    public static String formatDate(long milli) {
        return format(DateTimeFormatter.ISO_LOCAL_DATE, milli);
    }

    public static String formatTime(long milli) {
        return format(DateTimeFormatter.ISO_LOCAL_TIME, milli);
    }

    public static String formatZ(long milli) {
        return format(DateTimeFormatter.ISO_INSTANT, milli);
    }

    public static String formatFilename(long milli) { //! local time
        return format(fileFormatter, milli);
    }

    public static String formatLog(long milli) { //! local time
        return format(logFormatter, milli);
    }

    public static long parse(DateTimeFormatter formatter, String date) {
        return LocalDateTime.parse(date, formatter).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static long parse(String date) {
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignore) { // for Angelos
            return LocalDateTime.parse(date.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
    }

    public static long parseDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay() * DAY_IN_MILLIS;
    }

    /*
        public static long parseTime(String date) {
            return LocalTime.parse(date, DateTimeFormatter.ISO_LOCAL_TIME).toSecondOfDay() * 1000L;
        }
    */
    public static long optParse(String date, long alt) {
        if (date == null || date.isBlank())
            return alt;

        String spice = Spice.timeParse2UTC(date);
        if (spice != null) { // understood by SPICE, may still fail Java parser
            try {
                return parse(spice);
            } catch (Exception e) {
                return alt;
            }
        }
        // try NLP
        try {
            return roundSec(prettyParser.parse(date).getFirst().getTime());
        } catch (Exception e) {
            return alt;
        }
    }

    public static class SQLDateTimeFormatValidator implements FormatValidator {

        @Override
        public Optional<String> validate(String subject) {
            try {
                long time = parse(sqlTimeFormatter, subject);
                if (time < MINIMAL_TIME.milli || time > MAXIMAL_TIME.milli)
                    throw new Exception();

                return Optional.empty();
            } catch (DateTimeParseException e) {
                return Optional.of(String.format("[%s] is not a valid sql-date-time.", subject));
            } catch (Exception e) {
                return Optional.of(String.format("[%s] is outside time range of [%s,%s].", subject, MINIMAL_TIME, MAXIMAL_TIME));
            }
        }

        @Override
        public String formatName() {
            return "sql-date-time";
        }

    }

}
