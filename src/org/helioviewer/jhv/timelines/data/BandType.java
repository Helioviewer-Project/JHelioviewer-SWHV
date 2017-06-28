package org.helioviewer.jhv.timelines.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private static final HashMap<String, List<BandType>> groups = new HashMap<>();

    static void loadBandTypes(JSONArray jo) {
        for (int i = 0; i < jo.length(); i++) {
            BandType bandtype = new BandType(jo.getJSONObject(i));
            if (groups.containsKey(bandtype.group))
                groups.get(bandtype.group).add(bandtype);
            else
                groups.put(bandtype.group, new ArrayList<>(Collections.singletonList(bandtype)));
        }
    }

    public static BandType getBandType(String name) {
        for (List<BandType> list : groups.values()) {
            for (BandType bt : list)
                if (bt.getName().equals(name))
                    return bt;
        }
        return new BandType(new JSONObject("{}"));
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
    private final HashMap<String, Double> warnLevels = new HashMap<>();
    private double min = 0;
    private double max = 1;
    private boolean isLog = true;

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

        String scale = jo.optString("scale", "");
        if ("logarithmic".equals(scale))
            isLog = true;

        JSONArray warn = jo.optJSONArray("warnLevels");
        if (warn != null) {
            for (Object o : warn) {
                if (o instanceof JSONObject) {
                    JSONObject obj = (JSONObject) o;
                    warnLevels.put(obj.getString("warnLabel"), obj.getDouble("warnValue"));
                }
            }
        }
    }

    public void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public HashMap<String, Double> getWarnLevels() {
        return warnLevels;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public boolean isLogScale() {
        return isLog;
    }

    public String getBaseURL() {
        return baseURL;
    }

    @Override
    public String toString() {
        return label;
    }

   @Override
    public boolean equals(Object o) {
        return o instanceof BandType && o.toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
