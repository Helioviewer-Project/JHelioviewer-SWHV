package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nonnull;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.LinkedListMultimap;

public class BandType {

    private static final LinkedListMultimap<String, BandType> groups = LinkedListMultimap.create();

    static void loadBandTypes(List<BandType> types) {
        types.forEach(t -> groups.put(t.group, t));
    }

    static void loadBandTypes(JSONArray ja) {
        int len = ja.length();
        for (int i = 0; i < len; i++) {
            BandType bandType = new BandType(ja.getJSONObject(i));
            if (!"ODI".equals(bandType.group)) //! hide ODI for the time being
                groups.put(bandType.group, bandType);
        }
    }

    @Nonnull
    static BandType getBandType(String name) {
        for (BandType bt : groups.values()) {
            if (bt.name.equals(name))
                return bt;
        }
        return new BandType(new JSONObject());
    }

    @Nonnull
    public static List<BandType> getBandTypes(String group) {
        return groups.get(group);
    }

    @Nonnull
    public static String[] getGroups() {
        return groups.keySet().toArray(String[]::new);
    }

    private static final String[] XwarnLabels = new String[]{"B", "C", "M", "X"};
    private static final double[] XwarnValues = new double[]{1e-7, 1e-6, 1e-5, 1e-4};

    private final String name;
    private final String group;
    private final String baseURL;
    private final String label;
    private final String unitLabel;
    private final String[] warnLabels;
    private final double[] warnLevels;
    private final double min;
    private final double max;
    private final String scale;
    private final String bandCacheType;

    private final String dataset;
    private final String parameter;

    private final JSONObject json;

    BandType(JSONObject jo) {
        json = jo;

        name = jo.optString("name", "Unknown");
        group = jo.optString("group", "Unknown");
        baseURL = jo.optString("baseUrl", "");
        label = "<html><body>" + jo.optString("label", "Unknown").replaceAll("_(r|t|n|x|y|z|RTN|SRF|VSO|URF)", "<sub>$1</sub>");

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

        if (name.startsWith("GOES_XRSB")) { // temporary
            warnLabels = XwarnLabels;
            warnLevels = XwarnValues;
        } else {
            warnLabels = new String[0];
            warnLevels = new double[0];
        }

        bandCacheType = jo.optString("bandCacheType", "BandCacheMinute");

        dataset = jo.optString("dataset", "unknown");
        parameter = jo.optString("parameter", "unknown");
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

    @Nonnull
    String[] getWarnLabels() {
        return warnLabels;
    }

    @Nonnull
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

    String getDataset() {
        return dataset;
    }

    String getParameter() {
        return parameter;
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
