package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public record LUT(String name, int[] lut8) {

    public int[] lut8Inv() {
        int len = lut8.length;
        int[] inv = new int[len];

        for (int i = 0; i < len; i++) {
            inv[i] = lut8[len - 1 - i];
        }
        return inv;
    }

    private static final TreeMap<String, LUT> standardList = new TreeMap<>(JHVGlobals.alphanumComparator);
    // List of rules to apply
    private static JSONArray colorRules;

    static {
        LUTData.loadStandardLuts();

        // From the resources
        String[] ggrFiles = {"AIA94", "AIA131", "AIA171", "AIA193", "AIA211", "AIA304", "AIA335", "AIA1600", "AIA1700", "AIA4500"};
        for (String file : ggrFiles) {
            try (InputStream is = FileUtils.getResource("/ggr/" + file + ".ggr")) {
                LUT l = readGimpGradient(is);
                standardList.put(l.name, l);
            } catch (Exception e) {
                Log.warn("Could not restore gimp gradient file " + file, e);
            }
        }
        // read associations
        readColors();
    }

    private static LUT readGimpGradient(InputStream is) throws Exception {
        GimpGradient gg = new GimpGradient(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        int[] lut8 = new int[256];
        for (int i = 0; i < 256; i++) {
            lut8[i] = gg.getGradientColor(i / 255.);
        }
        return new LUT(gg.getName(), lut8);
    }

    static void addStdLut(String name, int... lookup8) {
        standardList.put(name, new LUT(name, lookup8));
    }

    private static void readColors() {
        try (InputStream is = FileUtils.getResource("/settings/colors.js");
             BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            colorRules = new JSONArray(new JSONTokener(in));
        } catch (IOException | JSONException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            colorRules = new JSONArray();
        }
    }

    public static String[] names() {
        return standardList.keySet().toArray(String[]::new);
    }

    public static LUT get(String name) {
        return standardList.get(name);
    }

    @Nullable
    public static LUT get(HelioviewerMetaData hvMetaData) {
        int length = colorRules.length();
        for (int i = 0; i < length; ++i) {
            try {
                JSONObject rule = colorRules.getJSONObject(i);
                if (rule.has("observatory") && !rule.getString("observatory").equalsIgnoreCase(hvMetaData.getObservatory()))
                    continue;
                if (rule.has("instrument") && !rule.getString("instrument").equalsIgnoreCase(hvMetaData.getInstrument()))
                    continue;
                if (rule.has("detector") && !rule.getString("detector").equalsIgnoreCase(hvMetaData.getDetector()))
                    continue;
                if (rule.has("measurement") && !rule.getString("measurement").equalsIgnoreCase(hvMetaData.getMeasurement()))
                    continue;
                return standardList.get(rule.getString("color"));
            } catch (JSONException e) {
                Log.warn("Rule " + i + " for the default color table is invalid", e);
            }
        }
        return null;
    }

}
