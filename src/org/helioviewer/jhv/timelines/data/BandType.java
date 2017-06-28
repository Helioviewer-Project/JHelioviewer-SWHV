package org.helioviewer.jhv.timelines.data;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private String name = "unknown";
    private String group = "unknown";
    private String baseURL = "";
    private String label = "";
    private String unitLabel = "";
    private final HashMap<String, Double> warnLevels = new HashMap<>();
    private double min = 0;
    private double max = 1;
    private boolean isLog;

    private final JSONObject json;

    BandType(JSONObject jo) {
        json = jo;

        name = jo.optString("name", name);
        group = jo.optString("group", group);
        baseURL = jo.optString("baseUrl", baseURL);
        label = jo.optString("label", label);
        unitLabel = jo.optString("unitLabel", unitLabel);

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

        System.out.println(">>> " + json);
    }

    public void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    @Override
    public String toString() {
        return label;
    }

    public String getLabel() {
        return label;
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

    public String getGroup() {
        return group;
    }

    public boolean isLogScale() {
        return isLog;
    }

    public String getBaseURL() {
        return baseURL;
    }

}
