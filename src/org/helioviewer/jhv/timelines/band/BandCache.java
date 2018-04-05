package org.helioviewer.jhv.timelines.band;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONObject;

public interface BandCache {

    boolean hasData();

    void addToCache(float[] values, long[] dates);

    void createPolyLines(Rectangle graphArea, TimeAxis timeAxis, YAxis yAxis, ArrayList<GraphPolyline> graphPolylines);

    float getValue(long ts);

    void serialize(JSONObject jo, double f);

    float[] getBounds(TimeAxis timeAxis);

    long getDepropagatedTime(long time);

}
