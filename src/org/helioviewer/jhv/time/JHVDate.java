package org.helioviewer.jhv.time;

import javax.annotation.Nonnull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class JHVDate implements Comparable<JHVDate> {

    public final long milli;
    private final int hash;

    public JHVDate(String date) {
        this(TimeUtils.parse(date));
    }

    public JHVDate(long _milli) {
        if (_milli < 0)
            throw new IllegalArgumentException("Argument cannot be negative");
        milli = _milli;
        hash = (int) (milli ^ (milli >>> 32));
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
        try {
            return cache.get(milli);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getString(milli);
    }

    private static final LoadingCache<Long, String> cache = CacheBuilder.newBuilder().maximumSize(100000).build(CacheLoader.from(JHVDate::getString));

    private static String getString(long ms) {
        return TimeUtils.format(ms);
    }

}
