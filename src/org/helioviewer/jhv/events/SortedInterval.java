package org.helioviewer.jhv.events;

import javax.annotation.Nonnull;

public class SortedInterval implements Comparable<SortedInterval> {

    public long start;
    public long end;
    private final int id;
    private static int id_gen = Integer.MIN_VALUE;

    public SortedInterval(long _start, long _end) {
        start = _start;
        end = _end;
        id = id_gen++;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SortedInterval && compareTo((SortedInterval) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        return 31 * result + id;
    }

    @Override
    public int compareTo(@Nonnull SortedInterval o2) {
        if (start < o2.start) {
            return -1;
        }
        if (start == o2.start && end < o2.end) {
            return -1;
        }
        if (start == o2.start && end == o2.end && o2.id < id) {
            return -1;
        }
        if (start == o2.start && end == o2.end && o2.id == id) {
            return 0;
        }
        return 1;
    }

}
