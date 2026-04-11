package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.metadata.FitsMetaData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public record LUT(String name, int[] lut8) {
    private static final String[] GGR_LUTS = {
            "AIA94", "AIA131", "AIA171", "AIA193", "AIA211",
            "AIA304", "AIA335", "AIA1600", "AIA1700", "AIA4500"
    };

    private record ColorRule(@Nullable String observatory, @Nullable String instrument, @Nullable String detector,
                             @Nullable String measurement, LUT lut) {
        private ColorRule {
            observatory = normalize(observatory);
            instrument = normalize(instrument);
            detector = normalize(detector);
            measurement = normalize(measurement);
        }

        boolean matches(FitsMetaData meta) {
            return matches(observatory, meta.getObservatory())
                    && matches(instrument, meta.getInstrument())
                    && matches(detector, meta.getDetector())
                    && matches(measurement, meta.getMeasurement());
        }

        private static boolean matches(@Nullable String expected, @Nullable String actual) {
            return expected == null || expected.equalsIgnoreCase(actual);
        }

        @Nullable
        private static String normalize(@Nullable String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }

    private static final Map<String, LUT> standardLuts = loadStandardLuts();
    private static final List<ColorRule> colorRules = readColorRules(standardLuts);

    public int[] lut8Inv() {
        int len = lut8.length;
        int[] inv = new int[len];

        for (int i = 0; i < len; i++) {
            inv[i] = lut8[len - 1 - i];
        }
        return inv;
    }

    private static Map<String, LUT> loadStandardLuts() {
        Map<String, LUT> luts = LUTReader.read("/luts/standard-luts.txt");

        for (String file : GGR_LUTS) {
            try (InputStream is = FileUtils.getResource("/luts/" + file + ".ggr")) {
                LUT l = readGimpGradient(is);
                luts.put(l.name, l);
            } catch (Exception e) {
                Log.warn("Could not restore gimp gradient file " + file, e);
            }
        }
        return luts;
    }

    private static LUT readGimpGradient(InputStream is) throws Exception {
        GimpGradient gg = new GimpGradient(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        int[] lut8 = new int[256];
        for (int i = 0; i < 256; i++) {
            lut8[i] = gg.getGradientColor(i / 255.);
        }
        return new LUT(gg.getName(), lut8);
    }

    private static List<ColorRule> readColorRules(Map<String, LUT> standardLuts) {
        try (InputStream is = FileUtils.getResource("/settings/colors.js");
             BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            JSONArray rules = new JSONArray(new JSONTokener(in));
            Set<ColorRule> parsedRules = new LinkedHashSet<>(rules.length());
            for (int i = 0; i < rules.length(); ++i) {
                try {
                    JSONObject rule = rules.getJSONObject(i);
                    String colorName = rule.getString("color");
                    LUT lut = standardLuts.get(colorName);
                    if (lut == null) {
                        Log.warn("Rule " + i + " for the default color table references missing LUT " + colorName);
                        continue;
                    }
                    ColorRule colorRule = new ColorRule(
                            rule.optString("observatory", null),
                            rule.optString("instrument", null),
                            rule.optString("detector", null),
                            rule.optString("measurement", null),
                            lut);
                    if (!parsedRules.add(colorRule)) {
                        Log.warn("Ignoring duplicate default color rule " + i);
                    }
                } catch (JSONException e) {
                    Log.warn("Rule " + i + " for the default color table is invalid", e);
                }
            }
            return new ArrayList<>(parsedRules);
        } catch (IOException | JSONException e) {
            Log.warn("Error reading the configuration for the default color tables", e);
            return List.of();
        }
    }

    @Nonnull
    public static LUT gray() { // invariant default for images
        return standardLuts.get("Gray");
    }

    @Nonnull
    public static LUT spectral() { // invariant default for radio
        return standardLuts.get("Spectral");
    }

    @Nonnull
    public static String[] names() {
        return standardLuts.keySet().toArray(String[]::new);
    }

    @Nullable
    public static LUT get(String name) {
        return standardLuts.get(name);
    }

    @Nullable
    public static LUT get(FitsMetaData meta) {
        for (ColorRule rule : colorRules) {
            if (rule.matches(meta))
                return rule.lut;
        }
        return null;
    }
}
