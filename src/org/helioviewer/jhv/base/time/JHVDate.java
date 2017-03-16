package org.helioviewer.jhv.base.time;

import org.jetbrains.annotations.NotNull;

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
    public int compareTo(@NotNull JHVDate dt) {
        return milli < dt.milli ? -1 : (milli > dt.milli ? +1 : 0);
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
