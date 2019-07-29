package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nonnull;

import org.json.JSONArray;

class DateValue implements Comparable<DateValue> {

    final long milli;
    final float value;

    DateValue(long _milli, float _value) {
        milli = _milli;
        value = _value;
    }

    void serialize(JSONArray ja, double f) {
        ja.put(new JSONArray().put(milli / 1000).put(value * f));
    }

    @Override
    public int compareTo(@Nonnull DateValue o) {
        return Long.compare(milli, o.milli);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DateValue))
            return false;
        DateValue d = (DateValue) o;
        return milli == d.milli;
    }

    @Override
    public int hashCode() {
        return (int) (milli ^ (milli >>> 32));
    }

}
