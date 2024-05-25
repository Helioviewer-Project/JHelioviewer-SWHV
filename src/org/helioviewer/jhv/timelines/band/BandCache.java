package org.helioviewer.jhv.timelines.band;

import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONObject;
import org.json.JSONArray;

interface BandCache {

    boolean hasData();

    void addToCache(YAxis yAxis, float[] values, long[] dates);

    float[] getBounds(long start, long end);

    List<List<DateValue>> getValues(double graphWidth, long start, long end);

    float getValue(long ts);

    void serialize(JSONObject jo, double f);

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

}
