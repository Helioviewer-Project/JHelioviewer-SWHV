package org.helioviewer.jhv.timelines.draw;

import java.awt.FontMetrics;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.time.TimeUtils;

public final class TimeTicks {

    public static final int LABEL_GAP = 16;

    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

    // Fixed steps divide a day and align to the epoch; month steps align to calendar months.
    public record Step(long millis, int months, DateTimeFormatter format, DateTimeFormatter dayFormat) {

        private long minDuration() {
            return months == 0 ? millis : months * 28 * TimeUtils.DAY_IN_MILLIS;
        }

        public long first(long start) {
            if (months == 0)
                return Math.floorDiv(start, millis) * millis;
            LocalDate month = Instant.ofEpochMilli(start).atOffset(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
            month = month.minusMonths(Math.floorMod(month.getYear() * 12 + month.getMonthValue() - 1, months));
            return month.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC) * 1000;
        }

        public long next(long tick) {
            return months == 0 ? tick + millis : Instant.ofEpochMilli(tick).atOffset(ZoneOffset.UTC).plusMonths(months).toInstant().toEpochMilli();
        }

        public String label(long tick, boolean withDate) {
            return TimeUtils.format(withDate ? dayFormat : format, tick);
        }
    }

    private static final List<Step> steps = createSteps();

    private static List<Step> createSteps() {
        List<Step> list = new ArrayList<>();
        for (int seconds : new int[]{1, 2, 5, 10, 15, 30})
            list.add(new Step(seconds * 1000L, 0, SECOND, TimeUtils.sqlTimeFormatter));
        for (int minutes : new int[]{1, 2, 5, 10, 15, 30, 60, 120, 180, 360, 720})
            list.add(new Step(minutes * TimeUtils.MINUTE_IN_MILLIS, 0, MINUTE, DATE_MINUTE));
        for (int days : new int[]{1, 2, 5, 10})
            list.add(new Step(days * TimeUtils.DAY_IN_MILLIS, 0, DAY, DAY));
        for (int months : new int[]{1, 2, 3, 6})
            list.add(new Step(0, months, MONTH, MONTH));
        for (int years : new int[]{1, 2, 5, 10, 20, 50})
            list.add(new Step(0, 12 * years, YEAR, YEAR));
        return list;
    }

    // The smallest step whose widest label fits between two ticks.
    public static Step step(FontMetrics fontMetrics, long start, long end, int width) {
        double pixelsPerMilli = width / (double) (end - start);
        for (Step step : steps) {
            if (step.minDuration() * pixelsPerMilli >= fontMetrics.stringWidth(step.label(start, true)) + LABEL_GAP)
                return step;
        }
        return steps.getLast();
    }

    private TimeTicks() {}

}
