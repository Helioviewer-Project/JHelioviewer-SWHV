package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.metadata.FitsMetaData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public record LUT(String name, ByteBuffer rgba) {
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

    public LUT(String name, int[] lut8) {
        this(name, packArgbToRgba(lut8));
    }

    public int[] lut8() {
        ByteBuffer source = rgba.duplicate();
        int[] lut8 = new int[source.remaining() / 4];
        for (int i = 0; i < lut8.length; i++) {
            int red = source.get() & 0xFF;
            int green = source.get() & 0xFF;
            int blue = source.get() & 0xFF;
            int alpha = source.get() & 0xFF;
            lut8[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return lut8;
    }

    @Override
    public ByteBuffer rgba() {
        return rgba.duplicate();
    }

    public ByteBuffer rgbaInv() {
        ByteBuffer source = rgba.duplicate();
        ByteBuffer inverted = BufferUtils.newByteBuffer(source.remaining());
        for (int pos = source.limit() - 4; pos >= 0; pos -= 4) {
            inverted.put(source.get(pos))
                    .put(source.get(pos + 1))
                    .put(source.get(pos + 2))
                    .put(source.get(pos + 3));
        }
        inverted.flip();
        return inverted;
    }

    public static LUT fromOpaqueRgb(String name, byte[] red, byte[] green, byte[] blue) {
        int len = red.length;
        ByteBuffer rgba = BufferUtils.newByteBuffer(len * 4);
        for (int i = 0; i < len; i++) {
            rgba.put(red[i]).put(green[i]).put(blue[i]).put((byte) 0xFF);
        }
        rgba.flip();
        return new LUT(name, rgba);
    }

    public static LUT fromOpaqueRgb(String name, float[] red, float[] green, float[] blue) {
        int len = red.length;
        ByteBuffer rgba = BufferUtils.newByteBuffer(len * 4);
        for (int i = 0; i < len; i++) {
            rgba.put((byte) ((red[i] + 0.5f) * 0xFF))
                    .put((byte) ((green[i] + 0.5f) * 0xFF))
                    .put((byte) ((blue[i] + 0.5f) * 0xFF))
                    .put((byte) 0xFF);
        }
        rgba.flip();
        return new LUT(name, rgba);
    }

    private static ByteBuffer packArgbToRgba(int[] argb) {
        ByteBuffer rgba = BufferUtils.newByteBuffer(argb.length * 4);
        for (int pixel : argb) {
            rgba.put((byte) (pixel >> 16))
                    .put((byte) (pixel >> 8))
                    .put((byte) pixel)
                    .put((byte) (pixel >>> 24));
        }
        rgba.flip();
        return rgba;
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
