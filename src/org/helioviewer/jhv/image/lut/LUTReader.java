package org.helioviewer.jhv.image.lut;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.NaturalSort;
import org.helioviewer.jhv.io.FileUtils;

final class LUTReader {
    private static final LUT gray = createGray();
    private static final LUT spectral = new LUT("Spectral", new int[]{
            -6422206, -6290622, -6159037, -6027197, -5895612, -5764028, -5632187, -5435067, -5303226, -5171642, -5040057, -4908217,
            -4776632, -4579256, -4447671, -4316087, -4184246, -4052662, -3920821, -3789237, -3592116, -3460276, -3328691, -3196851,
            -3065266, -2933682, -2802098, -2736050, -2604467, -2538419, -2472372, -2406324, -2340533, -2208949, -2142901, -2076854,
            -2010806, -1944759, -1813175, -1747384, -1681336, -1615289, -1483705, -1417658, -1351610, -1285819, -1219771, -1088188,
            -1022140, -956093, -890045, -758461, -757948, -757179, -691130, -690361, -689848, -623542, -623029, -622260, -556211,
            -555442, -554673, -488623, -487854, -487341, -421036, -420523, -354217, -353704, -352935, -286886, -286117, -285348,
            -219298, -218529, -218016, -151967, -151453, -150939, -150426, -149912, -149398, -148885, -148371, -147857, -147344,
            -146830, -146317, -145803, -145289, -144776, -144262, -143748, -143235, -142721, -142207, -141694, -141180, -140666,
            -140153, -139639, -73589, -73331, -73073, -72815, -72557, -72043, -71785, -71527, -71269, -71011, -70497, -70239, -69981,
            -69723, -69209, -68951, -68693, -68435, -68177, -67663, -67405, -67147, -66889, -66631, -66117, -65859, -65858, -131396,
            -196933, -262727, -328264, -394058, -459595, -525133, -590926, -656464, -722258, -787795, -853333, -919126, -984664,
            -1050457, -1115995, -1181532, -1247326, -1312863, -1378657, -1444194, -1509732, -1575525, -1641063, -1641064, -1837928,
            -1969256, -2100583, -2297447, -2428774, -2560102, -2756965, -2888293, -3019620, -3216484, -3347811, -3479139, -3676002,
            -3807330, -3938657, -4135521, -4266592, -4397920, -4529248, -4726111, -4857439, -4988766, -5185630, -5316957, -5448285,
            -5645148, -5842012, -5973340, -6170204, -6367068, -6498396, -6695260, -6892124, -7023452, -7220572, -7417436, -7614300,
            -7745628, -7942492, -8139356, -8270684, -8467548, -8664412, -8861276, -8992604, -9189468, -9386332, -9517660, -9714524,
            -9911388, -10042715, -10240091, -10371674, -10503257, -10635096, -10766679, -10898262, -11029845, -11161684, -11293267,
            -11424850, -11556689, -11688272, -11819855, -11951438, -12083277, -12214860, -12346443, -12478027, -12609866, -12741449,
            -12873032, -13004871, -13136454, -13268037, -13399620, -13465924, -13335365, -13204806, -13074247, -13009480, -12878921,
            -12748362, -12683339, -12552780, -12422478, -12291919, -12226896, -12096337, -11966034, -11835475, -11770452, -11639893,
            -11509590, -11444567, -11314008, -11183449, -11053146, -10988123, -10857564, -10727005, -10596446});

    private static LUT createGray() {
        int[] colors = new int[256];
        for (int i = 0; i < colors.length; i++)
            colors[i] = 0xFF000000 | i << 16 | i << 8 | i;
        return new LUT("Gray", colors);
    }

    static Map<String, LUT> read(String resourcePath) {
        TreeMap<String, LUT> luts = new TreeMap<>(NaturalSort.comparator);
        luts.put(gray.name(), gray);
        luts.put(spectral.name(), spectral);

        try (InputStream is = FileUtils.getResource(resourcePath)) {
            parse(is, luts);
        } catch (IOException e) {
            Log.warn("Could not read LUT resource " + resourcePath, e);
        }
        return luts;
    }

    static void read(File file, Map<String, LUT> luts) {
        if (!file.isFile())
            return;

        try (InputStream is = new FileInputStream(file)) {
            parse(is, luts);
        } catch (IOException e) {
            Log.warn("Could not read LUT file " + file, e);
        }
    }

    private static void parse(InputStream is, Map<String, LUT> luts) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String name = null;
            int nameLine = 0;
            List<String> dataLines = new ArrayList<>();
            String line;
            int lineNo = 0;

            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    if (name != null) {
                        addLut(luts, name, nameLine, dataLines);
                        name = null;
                        nameLine = 0;
                        dataLines.clear();
                    }
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    continue;
                }

                if (name == null) {
                    name = trimmed;
                    nameLine = lineNo;
                    continue;
                }

                dataLines.add(trimmed);
            }

            if (name != null) {
                addLut(luts, name, nameLine, dataLines);
            }
        }
    }

    private static void addLut(Map<String, LUT> luts, String name, int nameLine, List<String> dataLines) {
        if (luts.containsKey(name)) {
            Log.warn("Ignoring duplicate LUT '" + name + "' at line " + nameLine);
            return;
        }

        LUT lut = parseBlock(name, nameLine, dataLines);
        if (lut != null) {
            luts.put(name, lut);
        }
    }

    private static LUT parseBlock(String name, int nameLine, List<String> dataLines) {
        if (dataLines.isEmpty()) {
            Log.warn("Ignoring LUT '" + name + "' at line " + nameLine + ": no data");
            return null;
        }

        try {
            List<Integer> data = new ArrayList<>(256);
            for (int i = 0; i < dataLines.size(); i++) {
                parseDataLine(dataLines.get(i), data, nameLine + i + 1);
            }

            int[] values = new int[data.size()];
            for (int i = 0; i < data.size(); i++) {
                values[i] = data.get(i);
            }
            return new LUT(name, values);
        } catch (IOException e) {
            Log.warn("Ignoring invalid LUT '" + name + "' at line " + nameLine, e);
            return null;
        }
    }

    private static void parseDataLine(String line, List<Integer> data, int lineNo) throws IOException {
        String[] parts = line.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (value.isEmpty())
                throw new IOException("Empty LUT value at line " + lineNo);
            try {
                data.add(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid LUT value '" + value + "' at line " + lineNo, e);
            }
        }
    }

    private LUTReader() {}
}
