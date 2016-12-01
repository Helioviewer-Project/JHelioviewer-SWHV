package org.helioviewer.jhv.base.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JHVDate implements Comparable<JHVDate> {

    private final String string;
    public final long milli;

    public JHVDate(String date) {
        this(LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(TimeUtils.ZERO).toEpochMilli());
    }

    public JHVDate(long _milli) {
        if (_milli < 0)
            throw new IllegalArgumentException("Argument cannot be negative");
        milli = _milli;
        string = TimeUtils.utcDateFormat.format(milli);
    }

    @Override
    public int compareTo(JHVDate dt) {
        long diff = (milli - dt.milli);
        return diff < 0 ? -1 : (diff > 0 ? +1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JHVDate) {
            JHVDate d = (JHVDate) o;
            return milli == d.milli;
        }
        return false;
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
