package org.helioviewer.jhv.event;

import java.util.List;

import org.helioviewer.jhv.time.Interval;

public final class RelatedEventsTest {

    public static void main(String[] args) {
        JHVEvent first = new JHVEvent(null, 1, 100, 200);
        JHVEvent second = new JHVEvent(null, 2, 400, 500);
        JHVEventCache.addEvent(first);
        JHVEventCache.addEvent(second);
        JHVEventCache.addAssociation(new JHVEvent.Link(1, 2));
        JHVRelatedEvents group = JHVEventCache.getRelatedEvents(1);
        check(JHVEventCache.getEvents(300, 300).isEmpty(), "no phantom event in gap");
        check(JHVEventCache.getEvents(200, 200).size() == 1, "inclusive end");
        check(JHVEventCache.getEvents(400, 400).size() == 1, "inclusive start");
        check(group.getClosestTo(390) == second, "nearest event outside intervals");
        check(group.getIntervals().equals(List.of(new Interval(100, 200), new Interval(400, 500))), "timeline gaps");
        JHVEventCache.addEvent(new JHVEvent(null, 2, 190, 500));
        check(group.getIntervals().equals(List.of(new Interval(100, 500))), "overlapping interval union");
        check(JHVEventCache.getEvents(300, 300).size() == 1, "updated interval visible");
        System.out.println("RelatedEventsTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
