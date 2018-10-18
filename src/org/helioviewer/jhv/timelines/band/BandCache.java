package org.helioviewer.jhv.timelines.band;

import java.util.List;

import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONObject;

interface BandCache {

    boolean hasData();

    void addToCache(YAxis yAxis, float[] values, long[] dates);

    float[] getBounds(long start, long end);

    List<List<DateValue>> getValues(double graphWidth, long start, long end);

    float getValue(long ts);

    void serialize(JSONObject jo, double f);

}
