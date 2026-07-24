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
    private final WarningLevel[] warningLevels;
    private final double min;
    private final double max;
    private final String scale;
    private final String bandCacheType;
    private final boolean isXRSB;
    private record Level(double min, double max, Color color) {}

    record WarningLevel(String label, double value, Color color) {}

    record PredefinedEntry(String name, int order) {}

    private final PredefinedEntry[] predefinedEntries;
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
        predefinedEntries = parsePredefinedEntries(jo.optJSONArray("predefined"));

        plotType = jo.optString("plottype", null);
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

    String getBandCacheType() {
        return bandCacheType;
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

    String getPlotType() {
        return plotType;
    }

    long getBarWidth() {
        return barWidth;
    }

    boolean hasLevels() {
        return levels != null;
    }

    boolean hasWarningLevels() {
        return warningLevels.length > 0;
    }

    boolean isXRSB() {
        return isXRSB;
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

    private static Color namedColor(String name) {
        if (name.startsWith("#")) {
            try {
                return Color.decode(name);
            } catch (NumberFormatException e) {
                return null;
            }
        }
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
            Color color = namedColor(colorName);
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
