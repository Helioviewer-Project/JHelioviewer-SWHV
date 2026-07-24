package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Colors;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private static final Level[] NO_LEVELS = {};
    private static final String[] xWarnLabels = {"B", "C", "M", "X"};
    private static final double[] xWarnValues = {1e-7, 1e-6, 1e-5, 1e-4};

    private final String name;
    private final String baseUrl;
    private final String label;
    private final String unitLabel;
    private final WarningLevel[] warningLevels;
    private final double min;
    private final double max;
    private final String scale;
    private final CacheType cacheType;
    private final boolean isXRSB;

    private record Level(double min, double max, Color color) {}

    record WarningLevel(String label, double value, Color color) {}

    record PredefinedEntry(String name, int order) {}

    private final PredefinedEntry[] predefinedEntries;
    private final PlotType plotType;
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
        cacheType = CacheType.parse(jo.optString("bandCacheType", "BandCacheMinute"));
        predefinedEntries = parsePredefinedEntries(jo.optJSONArray("predefined"));

        plotType = PlotType.parse(jo.optString("plottype", null));
        barWidth = Math.max(0, jo.optLong("barWidth", 0));
        levels = parseLevels(jo.optJSONArray("levels"));

        isXRSB = label.contains("XRAY long");
        WarningLevel[] parsedWarningLevels = parseWarningLevels(jo.optJSONArray("warninglevels"));
        warningLevels = parsedWarningLevels.length == 0 && isXRSB
                ? defaultXRayWarningLevels()
                : parsedWarningLevels;
    }

    void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    String getName() {
        return name;
    }

    boolean cacheAllValues() {
        return cacheType == CacheType.ALL;
    }

    String getScale() {
        return scale;
    }

    String getUnitLabel() {
        return unitLabel;
    }

    WarningLevel[] getWarningLevels() {
        return warningLevels;
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

    PredefinedEntry[] getPredefinedEntries() {
        return predefinedEntries;
    }

    boolean isBarPlot() {
        return plotType == PlotType.BAR;
    }

    long getBarWidth() {
        return barWidth;
    }

    boolean hasLevels() {
        return levels.length > 0;
    }

    boolean hasWarningLevels() {
        return warningLevels.length > 0;
    }

    boolean isXRSB() {
        return isXRSB;
    }

    Color getLevelColor(double value) {
        for (Level level : levels) {
            if (value >= level.min && value <= level.max)
                return level.color;
        }
        return null;
    }

    private static Level[] parseLevels(JSONArray ja) {
        if (ja == null || ja.length() == 0)
            return NO_LEVELS;
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
            Color color = Colors.parseColor(colorName);
            if (color != null)
                list.add(new Level(min, max, color));
        }
        return list.isEmpty() ? NO_LEVELS : list.toArray(Level[]::new);
    }

    private static PredefinedEntry[] parsePredefinedEntries(JSONArray ja) {
        if (ja == null || ja.length() == 0)
            return new PredefinedEntry[0];

        ArrayList<PredefinedEntry> entries = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            Object value = ja.opt(i);
            if (value instanceof JSONObject jo) {
                String name = jo.optString("name", null);
                if (name != null)
                    entries.add(new PredefinedEntry(name, jo.optInt("order", 0)));
            } else if (value instanceof String name && !name.isBlank()) {
                entries.add(new PredefinedEntry(name, 0));
            }
        }
        return entries.toArray(PredefinedEntry[]::new);
    }

    private static WarningLevel[] parseWarningLevels(JSONArray ja) {
        if (ja == null || ja.length() == 0)
            return new WarningLevel[0];
        ArrayList<WarningLevel> list = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.optJSONObject(i);
            if (jo == null)
                continue;
            String label = jo.optString("label", null);
            double value = jo.optDouble("value", 0);
            String colorName = jo.optString("color", "white");
            Color color = Colors.parseColor(colorName);
            if (label != null && color != null)
                list.add(new WarningLevel(label, value, color));
        }
        return list.toArray(WarningLevel[]::new);
    }

    private static WarningLevel[] defaultXRayWarningLevels() {
        WarningLevel[] result = new WarningLevel[xWarnLabels.length];
        for (int i = 0; i < result.length; i++)
            result[i] = new WarningLevel(xWarnLabels[i], xWarnValues[i], null);
        return result;
    }

    private enum CacheType {
        ALL, MINUTE;

        static CacheType parse(String value) {
            return "BandCacheAll".equals(value) ? ALL : MINUTE;
        }
    }

    private enum PlotType {
        BAR, LINE;

        static PlotType parse(String value) {
            return "bar".equals(value) ? BAR : LINE;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BandType t))
            return false;
        return baseUrl.isEmpty()
                ? t.baseUrl.isEmpty() && name.equals(t.name)
                : baseUrl.equals(t.baseUrl);
    }

    @Override
    public int hashCode() {
        return baseUrl.isEmpty() ? name.hashCode() : baseUrl.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }

}
