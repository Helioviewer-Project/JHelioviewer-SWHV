package org.helioviewer.jhv.timelines.band;

import java.util.List;

import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.json.JSONObject;

interface BandCache {

    boolean hasData();

    void addToCache(float[] values, long[] dates);

    List<List<DateValue>> getValues(double graphWidth, TimeAxis timeAxis);

    float getValue(long ts);

    void serialize(JSONObject jo, double f);

    float[] getBounds(TimeAxis timeAxis);

}
