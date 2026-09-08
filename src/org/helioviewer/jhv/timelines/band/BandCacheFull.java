package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

class BandCacheFull implements BandCache {

    private final List<DateValue> dateVals = new ArrayList<>();

    @Override
    public boolean hasData() {
        return !dateVals.isEmpty();
    }

    @Override
    public void addToCache(YAxis yAxis, float[] values, long[] dates) {
        int len = values.length;
        if (len == 0) {
            return;
        }

        long previous = dateVals.isEmpty() ? Long.MIN_VALUE : dateVals.getLast().milli;
        boolean sortNeeded = false;
        for (int i = 0; i < len; i++) {
            if (dates[i] < previous)
                sortNeeded = true;
            dateVals.add(new DateValue(dates[i], yAxis.clip(values[i])));
            previous = dates[i];
        }
        if (sortNeeded)
            Collections.sort(dateVals);
    }

    @Override
    public float[] getBounds(long start, long end) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = firstIndexAtOrAfter(start); i < dateVals.size(); i++) {
            DateValue dv = dateVals.get(i);
            if (dv.milli > end)
                break;
            if (dv.value != YAxis.BLANK) {
                min = Math.min(dv.value, min);
                max = Math.max(dv.value, max);
            }
        }
        return new float[]{min, max};
    }

    @Override
    public List<List<DateValue>> getValues(double graphWidth, long start, long end) {
        List<List<DateValue>> ret = new ArrayList<>();
        int first = firstIndexAtOrAfter(start);
        int i = first;
        for (; i < dateVals.size(); i++) {
            DateValue dv = dateVals.get(i);
            if (dv.milli > end)
                break;
            if (dv.value == YAxis.BLANK) {
                if (first < i)
                    ret.add(List.copyOf(dateVals.subList(first, i)));
                first = i + 1;
            }
        }
        if (first < i)
            ret.add(List.copyOf(dateVals.subList(first, i)));
        return ret;
    }

    @Override
    public float getValue(long ts) {
        if (dateVals.isEmpty() || ts < dateVals.getFirst().milli || ts > dateVals.getLast().milli)
            return YAxis.BLANK;

        return dateVals.get(firstIndexAtOrAfter(ts)).value;
    }

    private int firstIndexAtOrAfter(long ts) {
        int low = 0, high = dateVals.size();
        while (low != high) {
            int mid = (low + high) / 2;
            if (dateVals.get(mid).milli < ts) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return high;
    }

    @Override
    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        dateVals.forEach(dv -> dv.serialize(ja, f));
        jo.put("data", ja);
    }

}
