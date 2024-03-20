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
        ja.put(new JSONArray().put(milli / 1000L).put(value * f));
    }

    @Override
    public int compareTo(@Nonnull DateValue o) {
        return Long.compare(milli, o.milli);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof DateValue v)
            return milli == v.milli;
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(milli);
    }

}
