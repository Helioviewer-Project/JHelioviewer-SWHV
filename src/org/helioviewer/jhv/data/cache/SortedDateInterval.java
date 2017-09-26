package org.helioviewer.jhv.data.cache;

import javax.annotation.Nonnull;

public class SortedDateInterval implements Comparable<SortedDateInterval> {

    public long start;
    public long end;
    private final int id;
    private static int id_gen = Integer.MIN_VALUE;

    public SortedDateInterval(long _start, long _end) {
        start = _start;
        end = _end;
        id = id_gen++;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SortedDateInterval && compareTo((SortedDateInterval) o) == 0;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public int compareTo(@Nonnull SortedDateInterval o2) {
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
