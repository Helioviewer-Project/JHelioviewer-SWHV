package org.helioviewer.jhv.timelines.draw;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.time.TimeUtils;

public final class TimeAxis {

    private long start;
    private long end;

    public TimeAxis(long _start, long _end) {
        set(_start, _end);
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public Mapper mapper(int x0, int width) {
        return new Mapper(start, end, x0, width);
    }

    void move(int w, double pixelDistance) {
        long diff = (long) ((end - start) / (double) w * pixelDistance);
        move(diff);
    }

    void move(long diff) {
        start += diff;
        end += diff;
        adaptBounds();
    }

    void zoom(int x0, int w, int x, double factor) {
        double multiplier = (end - start) / (double) w * factor;
        double ratio = (x - x0) / (double) w;
        start -= (long) (multiplier * ratio);
        end += (long) (multiplier * (1. - ratio));
        adaptBounds();
    }

    void set(long _start, long _end) {
        start = _start;
        end = _end;
        adaptBounds();
    }

    private void adaptBounds() {
        long now = System.currentTimeMillis(); // assume now < TimeUtils.MAXIMAL_TIME

        start = Math.clamp(start, TimeUtils.MINIMAL_TIME.milli, now);
        end = Math.clamp(end, Math.min(start + TimeUtils.MINUTE_IN_MILLIS, now), now);
        if (end - start < TimeUtils.MINUTE_IN_MILLIS) {
            start = now - TimeUtils.MINUTE_IN_MILLIS;
            end = now;
        }
    }

    // Display ticks only. The movie interval and sample timestamps remain exact.
    public List<Long> ticks(int maximum) {
        int count = Math.max(2, maximum);
        long target = Math.max(1, (end - start) / (count - 1));
        long second = 1000;
        long minute = TimeUtils.MINUTE_IN_MILLIS;
        long hour = 60 * minute;
        long day = TimeUtils.DAY_IN_MILLIS;
        long[] steps = {second, 2 * second, 5 * second, 10 * second, 15 * second, 30 * second,
                minute, 2 * minute, 5 * minute, 10 * minute, 15 * minute, 30 * minute,
                hour, 2 * hour, 3 * hour, 6 * hour, 12 * hour, day, 2 * day, 7 * day, 14 * day};
        List<Long> ticks = new ArrayList<>();
        if (target < 32 * day) {
            long step = steps[0];
            for (long candidate : steps) {
                step = candidate;
                if (step >= target)
                    break;
            }
            long first = Math.floorDiv(start, step) * step;
            if (first < start)
                first += step;
            for (long tick = first; tick <= end; tick += step)
                ticks.add(tick);
        } else {
            int months = 1;
            int[] monthSteps = {1, 2, 3, 6, 12, 24, 60, 120, 240, 600, 1200, 2400};
            for (int candidate : monthSteps) {
                months = candidate;
                if (months * (365.2425 / 12) * day >= target)
                    break;
            }
            ZonedDateTime date = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC);
            int index = date.getYear() * 12 + date.getMonthValue() - 1;
            int aligned = Math.floorDiv(index, months) * months;
            ZonedDateTime tick = ZonedDateTime.of(Math.floorDiv(aligned, 12), Math.floorMod(aligned, 12) + 1,
                    1, 0, 0, 0, 0, ZoneOffset.UTC);
            if (tick.toInstant().toEpochMilli() < start)
                tick = tick.plusMonths(months);
            // A very narrow plot still gets a calendar tick within its interval.
            if (tick.toInstant().toEpochMilli() > end && months > 1)
                return ticks(count + 1);
            while (tick.toInstant().toEpochMilli() <= end) {
                ticks.add(tick.toInstant().toEpochMilli());
                tick = tick.plusMonths(months);
            }
        }
        return ticks;
    }

    public record Mapper(long start, long end, int x0, int width) {
        public int toPixel(long value) {
            return (int) ((double) width / (end - start) * (value - start) + x0);
        }

        public long toValue(int pixel) {
            return (long) (start + (end - start) / (double) width * (pixel - x0));
        }
    }

}
