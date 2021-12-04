package org.helioviewer.jhv.time;

public class TimeListener {

    public interface Change {
        void timeChanged(long milli);
    }

    public interface Range {
        void timeRangeChanged(long start, long end);
    }

    public interface Selection {
        void timeSelectionChanged(long start, long end);
    }

}
