package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private static final HashMap<String, List<BandType>> groups = new HashMap<>();

    static void loadBandTypes(JSONArray jo) {
        int len = jo.length();
        for (int i = 0; i < len; i++) {
            BandType bandtype = new BandType(jo.getJSONObject(i));
            if (groups.containsKey(bandtype.group))
                groups.get(bandtype.group).add(bandtype);
            else
                groups.put(bandtype.group, new ArrayList<>(Collections.singletonList(bandtype)));
        }
    }

    static BandType getBandType(String name) {
        for (List<BandType> list : groups.values()) {
            for (BandType bt : list)
                if (bt.name.equals(name))
                    return bt;
        }
        return new BandType(new JSONObject());
    }

    public static List<BandType> getBandTypes(String group) {
        List<BandType> list = groups.get(group);
        return list == null ? new ArrayList<>() : list;
    }

    public static String[] getGroups() {
        return groups.keySet().toArray(new String[0]);
    }

    private String name = "unknown";
    private String group = "unknown";
    private String baseURL = "";
    private String label = "Unknown";
    private String unitLabel = "unknown";
    private String[] warnLabels = new String[0];
    private double[] warnLevels = new double[0];
    private double min = 0;
    private double max = 1;
    private String scale = "linear";
    private String bandCacheType = "BandCacheMinute";

    private final JSONObject json;

    BandType(JSONObject jo) {
        json = jo;

        name = jo.optString("name", name);
        group = jo.optString("group", group);
        baseURL = jo.optString("baseUrl", baseURL);
        label = jo.optString("label", label);

        unitLabel = jo.optString("unitLabel", unitLabel);
        if ("".equals(unitLabel)) // crashes ChartDrawGraphPane.drawVerticalLabels
            unitLabel = " ";

        JSONArray range = jo.optJSONArray("range");
        if (range != null) {
            min = range.optDouble(0, min);
            max = range.optDouble(1, max);
        }

        scale = jo.optString("scale", scale);

        JSONArray warn = jo.optJSONArray("warnLevels");
        if (warn != null) {
            int len = warn.length();
            warnLabels = new String[len];
            warnLevels = new double[len];
            for (int i = 0; i < len; i++) {
                JSONObject o = warn.getJSONObject(i);
                warnLabels[i] = o.getString("warnLabel");
                warnLevels[i] = o.getDouble("warnValue");
            }
        }

        bandCacheType = jo.optString("bandCacheType", bandCacheType);
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

    String getBaseURL() {
        return baseURL;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BandType))
            return false;
        BandType t = (BandType) o;
        return name.equals(t.name);
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
