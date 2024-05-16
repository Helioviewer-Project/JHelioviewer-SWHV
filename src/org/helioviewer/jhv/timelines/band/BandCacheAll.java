package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONArray;
import org.json.JSONObject;

class BandCacheAll implements BandCache {

    private static final int MAX_SIZE = 86400;

    private final List<DateValue> dateVals = new ArrayList<>();
    private boolean hasData;
    private DateValue first;
    private DateValue last;

    @Override
    public boolean hasData() {
        return hasData;
    }

    @Override
    public void addToCache(YAxis yAxis, float[] values, long[] dates) {
        int len = values.length;
        if (len == 0 || dateVals.size() >= MAX_SIZE) {
            return;
        }

        hasData = true;
        for (int i = 0; i < len; i++) {
            dateVals.add(new DateValue(dates[i], yAxis.clip(values[i])));
            if (dateVals.size() >= MAX_SIZE)
                break;
        }
        Collections.sort(dateVals);

        first = dateVals.get(0);
        last = dateVals.get(dateVals.size() - 1);
    }

    @Override
    public float[] getBounds(long start, long end) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (DateValue dv : dateVals) {
            if (dv.value != YAxis.BLANK && start <= dv.milli && dv.milli <= end) {
                min = Math.min(dv.value, min);
                max = Math.max(dv.value, max);
            }
        }
        return new float[]{min, max};
    }

    @Override
    public List<List<DateValue>> getValues(double graphWidth, long start, long end) {
        List<List<DateValue>> ret = new ArrayList<>();
        List<DateValue> list = new ArrayList<>();
        for (DateValue dv : dateVals) {
            if (dv.value == YAxis.BLANK) {
                ret.add(list);
                list = new ArrayList<>();
            } else if (start <= dv.milli && dv.milli <= end) {
                list.add(dv);
            }
        }
        ret.add(list);
        return ret;
    }

    @Override
    public float getValue(long ts) {
        if (first == null || ts < first.milli || ts > last.milli) // if first is not null, last cannot be null
            return YAxis.BLANK;
        int low = 0, high = dateVals.size();
        while (low != high) {
            int mid = (low + high) / 2;
            if (dateVals.get(mid).milli <= ts) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return dateVals.get(high).value;
    }

    @Override
    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        dateVals.forEach(dv -> dv.serialize(ja, f));
        jo.put("data", ja);
    }

}
