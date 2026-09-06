package org.helioviewer.jhv.time;

import java.util.ArrayList;
import java.util.List;

public final class IntervalTest {

    public static void main(String[] args) {
        check(merge().isEmpty(), "empty coverage");
        check(merge(new Interval(5, 5)).equals(List.of(new Interval(5, 5))), "point interval survives");
        check(merge(new Interval(20, 30), new Interval(0, 10), new Interval(10, 20), new Interval(5, 15), new Interval(0, 10))
                .equals(List.of(new Interval(0, 30))), "unsorted, touching, overlapping and duplicate intervals form one union");
        check(merge(new Interval(10, 20), new Interval(-10, -5), new Interval(0, 0), new Interval(11, 12))
                .equals(List.of(new Interval(-10, -5), new Interval(0, 0), new Interval(10, 20))), "gaps and isolated points survive nested intervals");
        check(merge(new Interval(0, Long.MAX_VALUE), new Interval(Long.MIN_VALUE, 0))
                .equals(List.of(new Interval(Long.MIN_VALUE, Long.MAX_VALUE))), "endpoint merging does not require arithmetic that can overflow");
        RequestCache cache = new RequestCache();
        cache.adaptRequestCache(10, 20);
        cache.adaptRequestCache(20, 30);
        check(cache.getAllRequestIntervals().equals(List.of(new Interval(10, 30))), "request cache merges touching coverage");
        cache.removeRequestedInterval(15, 25);
        check(cache.getAllRequestIntervals().equals(List.of(new Interval(10, 15), new Interval(25, 30))), "subtraction preserves the remaining coverage");
        check(cache.getMissingIntervals(10, 30).equals(List.of(new Interval(15, 25))), "removed coverage is requestable again");
        System.out.println("IntervalTest passed");
    }

    private static List<Interval> merge(Interval... intervals) {
        return Interval.merge(new ArrayList<>(List.of(intervals)));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
