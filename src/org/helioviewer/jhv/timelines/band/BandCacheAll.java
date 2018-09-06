package org.helioviewer.jhv.timelines.band;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.helioviewer.jhv.timelines.propagation.PropagationModel;
import org.json.JSONArray;
import org.json.JSONObject;

class BandCacheAll implements BandCache {

    private final ArrayList<DateValue> datevals = new ArrayList<>();
    private boolean hasData;
    private PropagationModel propagationModel;

    @Override
    public boolean hasData() {
        return hasData;
    }

    @Override
    public void addToCache(float[] values, long[] dates) {
        int len = values.length;
        if (len > 0) {
            hasData = true;
        }
        int MAX_SIZE = 10000;
        if (datevals.size() >= MAX_SIZE) {
            return;
        }

        for (int i = 0; i < len; i++) {
            if (datevals.size() >= MAX_SIZE)
                break;
            datevals.add(new DateValue(dates[i], values[i]));
        }
        Collections.sort(datevals);
    }

    @Override
    public float[] getBounds(TimeAxis timeAxis) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (DateValue dv : datevals) {
            if (dv.value != Float.MIN_VALUE && timeAxis.start <= dv.milli && dv.milli <= timeAxis.end) {
                min = Math.min(dv.value, min);
                max = Math.max(dv.value, max);
            }
        }
        return new float[]{min, max};
    }

    @Override
    public void createPolyLines(Rectangle graphArea, TimeAxis timeAxis, YAxis yAxis,
                                ArrayList<GraphPolyline> graphPolylines) {

        ArrayList<Integer> tvalues = new ArrayList<>();
        ArrayList<Integer> tdates = new ArrayList<>();
        for (DateValue dv : datevals) {
            if (dv.value != Float.MIN_VALUE && timeAxis.start <= dv.milli && dv.milli <= timeAxis.end) {
                tdates.add(timeAxis.value2pixel(graphArea.x, graphArea.width, dv.milli));
                tvalues.add(yAxis.value2pixel(graphArea.y, graphArea.height, dv.value));
            }
        }
        if (!tvalues.isEmpty()) {
            graphPolylines.add(new GraphPolyline(tdates, tvalues));
        }
    }

    @Override
    public float getValue(long ts) {
        return Float.MIN_VALUE;
    }

    @Override
    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        for (DateValue dv : datevals)
            dv.serialize(ja, f);
        jo.put("data", ja);
    }

    private static class DateValue implements Comparable<DateValue> {

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
        public boolean equals(Object o) {
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

    @Override
    public PropagationModel getPropagationModel() {
        return propagationModel;
    }

    @Override
    public void setPropagationModel(PropagationModel pm) {
        propagationModel = pm;
    }

}
