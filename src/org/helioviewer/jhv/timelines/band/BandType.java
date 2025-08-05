package org.helioviewer.jhv.timelines.band;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private static final String[] xWarnLabels = {"B", "C", "M", "X"};
    private static final double[] xWarnValues = {1e-7, 1e-6, 1e-5, 1e-4};

    private final String name;
    private final String baseUrl;
    private final String label;
    private final String unitLabel;
    private final String[] warnLabels;
    private final double[] warnLevels;
    private final double min;
    private final double max;
    private final String scale;
    private final String bandCacheType;
    private final boolean isXRSB;

    private final JSONObject json;

    BandType(JSONObject jo) {
        json = jo;

        name = jo.optString("name", "Unknown");
        baseUrl = jo.optString("baseUrl", "");
        label = jo.optString("label", "Unknown");

        String ul = jo.optString("unitLabel", "unknown");
        if ("".equals(ul)) // crashes ChartDrawGraphPane.drawVerticalLabels
            ul = " ";
        unitLabel = ul;

        JSONArray range = jo.optJSONArray("range");
        if (range != null) {
            min = range.optDouble(0, 0);
            max = range.optDouble(1, 1);
        } else {
            min = 0;
            max = 1;
        }

        scale = jo.optString("scale", "linear");
        bandCacheType = jo.optString("bandCacheType", "BandCacheMinute");

        isXRSB = label.contains("XRAY long");
        if (isXRSB) {
            warnLabels = xWarnLabels;
            warnLevels = xWarnValues;
        } else {
            warnLabels = new String[0];
            warnLevels = new double[0];
        }
    }

    void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    String getName() {
        return name;
    }

    String getBandCacheType() {
        return bandCacheType;
    }

    String getScale() {
        return scale;
    }

    String getUnitLabel() {
        return unitLabel;
    }

    String[] getWarnLabels() {
        return warnLabels;
    }

    double[] getWarnLevels() {
        return warnLevels;
    }

    double getMin() {
        return min;
    }

    double getMax() {
        return max;
    }

    String getBaseUrl() {
        return baseUrl;
    }

    boolean isXRSB() {
        return isXRSB;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof BandType t)
            return name.equals(t.name);
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }

}
