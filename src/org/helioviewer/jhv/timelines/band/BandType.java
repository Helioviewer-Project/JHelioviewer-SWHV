package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.util.ArrayList;

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
    private record Level(double min, double max, Color color) {}

    private final String predefinedGroup;
    private final int predefinedOrder;
    private final String plotType;
    private final long barWidth;
    private final Level[] levels;

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
        predefinedGroup = jo.optString("predefinedGroup", null);
        predefinedOrder = jo.optInt("predefinedOrder", 0);

        plotType = jo.optString("plottype", null);
        barWidth = jo.optLong("barWidth", 0);
        levels = parseLevels(jo.optJSONArray("levels"));

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

    String getPredefinedGroup() {
        return predefinedGroup;
    }

    int getPredefinedOrder() {
        return predefinedOrder;
    }

    String getPlotType() {
        return plotType;
    }

    long getBarWidth() {
        return barWidth;
    }

    Level[] getLevels() {
        return levels;
    }

    Color getLevelColor(double value) {
        if (levels == null)
            return null;
        for (Level level : levels) {
            if (value >= level.min && value <= level.max)
                return level.color;
        }
        return null;
    }

    private static Level[] parseLevels(JSONArray ja) {
        if (ja == null || ja.length() == 0)
            return null;
        ArrayList<Level> list = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.optJSONObject(i);
            if (jo == null)
                continue;
            JSONArray range = jo.optJSONArray("range");
            if (range == null || range.length() < 2)
                continue;
            double min = range.optDouble(0, -Double.MAX_VALUE);
            Object maxObj = range.opt(1);
            double max;
            if (maxObj instanceof String s && "infinity".equals(s))
                max = Double.MAX_VALUE;
            else
                max = range.optDouble(1, Double.MAX_VALUE);
            String colorName = jo.optString("color", "black");
            Color color = namedColor(colorName);
            if (color != null)
                list.add(new Level(min, max, color));
        }
        return list.isEmpty() ? null : list.toArray(Level[]::new);
    }

    private static Color namedColor(String name) {
        return switch (name.toLowerCase()) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "orange" -> Color.ORANGE;
            case "cyan" -> Color.CYAN;
            case "magenta" -> Color.MAGENTA;
            case "white" -> Color.WHITE;
            case "black" -> Color.BLACK;
            case "gray", "grey" -> Color.GRAY;
            case "pink" -> Color.PINK;
            default -> null;
        };
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
