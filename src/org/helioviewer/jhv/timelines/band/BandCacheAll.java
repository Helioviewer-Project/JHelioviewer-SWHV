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

    private final ArrayList<DateVal> datevals = new ArrayList<>();
    private boolean hasData;
    private PropagationModel propagationModel;

    public boolean hasData() {
        return hasData;
    }

    public void addToCache(float[] values, long[] dates) {
        if (values.length != 0) {
            hasData = true;
        }
        int MAX_SIZE = 10000;
        if (datevals.size() >= MAX_SIZE) {
            return;
        }

        for (int i = 0; i < values.length; i++) {
            if (datevals.size() >= MAX_SIZE)
                break;
            datevals.add(new DateVal(dates[i], values[i]));
        }
        Collections.sort(datevals);
    }

    public float[] getBounds(TimeAxis timeAxis) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (DateVal dv : datevals) {
            if (dv.val != Float.MIN_VALUE && timeAxis.start <= dv.date && dv.date <= timeAxis.end) {
                min = Math.min(dv.val, min);
                max = Math.max(dv.val, max);
            }
        }
        return new float[] { min, max };
    }

    public void createPolyLines(Rectangle graphArea, TimeAxis timeAxis, YAxis yAxis,
            ArrayList<GraphPolyline> graphPolylines) {

        ArrayList<Integer> tvalues = new ArrayList<>();
        ArrayList<Integer> tdates = new ArrayList<>();
        for (DateVal dv : datevals) {
            if (dv.val != Float.MIN_VALUE && timeAxis.start <= dv.date && dv.date <= timeAxis.end) {
                tdates.add(timeAxis.value2pixel(graphArea.x, graphArea.width, dv.date));
                tvalues.add(yAxis.value2pixel(graphArea.y, graphArea.height, dv.val));
            }
        }
        if (!tvalues.isEmpty()) {
            graphPolylines.add(new GraphPolyline(tdates, tvalues));
        }
    }

    public float getValue(long ts) {
        return Float.MIN_VALUE;
    }

    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        for (DateVal dv : datevals)
            dv.serialize(ja, f);
        jo.put("data", ja);
    }

    private static class DateVal implements Comparable<DateVal> {

        public long date;
        public float val;

        public DateVal(long _date, float _val) {
            date = _date;
            val = _val;
        }

        @Override
        public int compareTo(@Nonnull DateVal o) {
            return Long.compare(date, o.date);
        }

        void serialize(JSONArray ja, double f) {
            ja.put(new JSONArray().put(date / 1000).put(val * f));
        }

    }

    public PropagationModel getPropagationModel() {
        return propagationModel;
    }

    public void setPropagationModel(PropagationModel pm) {
        propagationModel = pm;
    }
}
