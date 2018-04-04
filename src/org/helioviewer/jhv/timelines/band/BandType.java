package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.timelines.draw.YAxis.YAxisScale;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisLogScale;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisPositiveIdentityScale;
import org.helioviewer.jhv.timelines.draw.YAxis.YAxisIdentityScale;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {
    public enum Scale {
        Logarithmic, Linear, PositiveLinear;

        YAxisScale generateScale(String _label) {
            switch (this) {
            case Logarithmic:
                return new YAxisLogScale(_label);
            case Linear:
                return new YAxisIdentityScale(_label);
            case PositiveLinear:
                return new YAxisPositiveIdentityScale(_label);
            default:
                return new YAxisIdentityScale(_label);
            }
        }
    }
    private static final HashMap<String, List<BandType>> groups = new HashMap<>();
    
    private Scale scale = Scale.Linear;
    
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
                if (bt.name.equals(name))
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
    private final Map<String, Double> warnLevels;
    private double min = 0;
    private double max = 1;
    private String bandCacheType = "BandCacheMinute";

    private final JSONObject json;

    public BandType(JSONObject jo) {
        
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

        String scale_str = jo.optString("scale", "");
        if ("logarithmic".equals(scale_str))
            scale = Scale.Logarithmic;
        else if ("linear".equals(scale_str))
            scale = Scale.Linear;
        else
            scale = Scale.PositiveLinear;

        JSONArray warn = jo.optJSONArray("warnLevels");
        HashMap<String, Double> warnHelp = new HashMap<>();
        if (warn != null) {
            for (Object o : warn) {
                if (o instanceof JSONObject) {
                    JSONObject obj = (JSONObject) o;
                    warnHelp.put(obj.getString("warnLabel"), obj.getDouble("warnValue"));
                }
            }
        }
        bandCacheType = jo.optString("bandCacheType", "BandCacheMinute");

        warnLevels = Collections.unmodifiableMap(warnHelp);
    }

    public void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    public String getName() {
        return name;
    }
    
    public String getBandCacheType() {
        return bandCacheType;
    }
    
    public String getUnitLabel() {
        return unitLabel;
    }

    public Map<String, Double> getWarnLevels() {
        return warnLevels;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public YAxisScale getScale(String label) {
        return scale.generateScale(label);
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
        if (!(o instanceof BandType))
            return false;
        BandType t = (BandType) o;
        return name.equals(t.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}

