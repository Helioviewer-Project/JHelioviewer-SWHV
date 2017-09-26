package org.helioviewer.jhv.time;

import javax.annotation.Nonnull;

public class JHVDate implements Comparable<JHVDate> {

    private final String string;
    public final long milli;

    public JHVDate(String date) {
        this(TimeUtils.parse(date));
    }

    public JHVDate(long _milli) {
        if (_milli < 0)
            throw new IllegalArgumentException("Argument cannot be negative");
        milli = _milli;
        string = TimeUtils.format(milli);
    }

    @Override
    public int compareTo(@Nonnull JHVDate dt) {
        return Long.compare(milli, dt.milli);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHVDate))
            return false;
        JHVDate d = (JHVDate) o;
        return milli == d.milli;
    }

    @Override
    public int hashCode() {
        return (int) (milli ^ (milli >>> 32));
    }

    @Override
    public String toString() {
        return string;
    }

}
