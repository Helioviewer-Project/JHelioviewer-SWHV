package org.helioviewer.jhv.time;

import javax.annotation.Nonnull;

public class JHVDate implements Comparable<JHVDate> {

    public final long milli;
    private final int hash;
    private final String string;

    public JHVDate(String date) {
        this(TimeUtils.parse(date));
    }

    public JHVDate(long _milli) {
        if (_milli < 0)
            throw new IllegalArgumentException("Argument cannot be negative");
        milli = _milli;
        hash = (int) (milli ^ (milli >>> 32));
        string = TimeUtils.format(milli);
    }

    @Override
    public int compareTo(@Nonnull JHVDate dt) {
        return Long.compare(milli, dt.milli);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JHVDate))
            return false;
        JHVDate d = (JHVDate) o;
        return milli == d.milli;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return string;
    }

}
