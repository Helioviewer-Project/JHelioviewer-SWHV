package org.helioviewer.jhv.base.lut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.FileUtils;

final class LUTReader {
    private static final LUT gray = new LUT("Gray", new int[]{
            -16777216, -16711423, -16645630, -16579837, -16514044, -16448251, -16382458, -16316665, -16250872, -16185079, -16119286,
            -16053493, -15987700, -15921907, -15856114, -15790321, -15724528, -15658735, -15592942, -15527149, -15461356, -15395563,
            -15329770, -15263977, -15198184, -15132391, -15066598, -15000805, -14935012, -14869219, -14803426, -14737633, -14671840,
            -14606047, -14540254, -14474461, -14408668, -14342875, -14277082, -14211289, -14145496, -14079703, -14013910, -13948117,
            -13882324, -13816531, -13750738, -13684945, -13619152, -13553359, -13487566, -13421773, -13355980, -13290187, -13224394,
            -13158601, -13092808, -13027015, -12961222, -12895429, -12829636, -12763843, -12698050, -12632257, -12566464, -12500671,
            -12434878, -12369085, -12303292, -12237499, -12171706, -12105913, -12040120, -11974327, -11908534, -11842741, -11776948,
            -11711155, -11645362, -11579569, -11513776, -11447983, -11382190, -11316397, -11250604, -11184811, -11119018, -11053225,
            -10987432, -10921639, -10855846, -10790053, -10724260, -10658467, -10592674, -10526881, -10461088, -10395295, -10329502,
            -10263709, -10197916, -10132123, -10066330, -10000537, -9934744, -9868951, -9803158, -9737365, -9671572, -9605779, -9539986,
            -9474193, -9408400, -9342607, -9276814, -9211021, -9145228, -9079435, -9013642, -8947849, -8882056, -8816263, -8750470,
            -8684677, -8618884, -8553091, -8487298, -8421505, -8355712, -8289919, -8224126, -8158333, -8092540, -8026747, -7960954,
            -7895161, -7829368, -7763575, -7697782, -7631989, -7566196, -7500403, -7434610, -7368817, -7303024, -7237231, -7171438,
            -7105645, -7039852, -6974059, -6908266, -6842473, -6776680, -6710887, -6645094, -6579301, -6513508, -6447715, -6381922,
            -6316129, -6250336, -6184543, -6118750, -6052957, -5987164, -5921371, -5855578, -5789785, -5723992, -5658199, -5592406,
            -5526613, -5460820, -5395027, -5329234, -5263441, -5197648, -5131855, -5066062, -5000269, -4934476, -4868683, -4802890,
            -4737097, -4671304, -4605511, -4539718, -4473925, -4408132, -4342339, -4276546, -4210753, -4144960, -4079167, -4013374,
            -3947581, -3881788, -3815995, -3750202, -3684409, -3618616, -3552823, -3487030, -3421237, -3355444, -3289651, -3223858,
            -3158065, -3092272, -3026479, -2960686, -2894893, -2829100, -2763307, -2697514, -2631721, -2565928, -2500135, -2434342,
            -2368549, -2302756, -2236963, -2171170, -2105377, -2039584, -1973791, -1907998, -1842205, -1776412, -1710619, -1644826,
            -1579033, -1513240, -1447447, -1381654, -1315861, -1250068, -1184275, -1118482, -1052689, -986896, -921103, -855310,
            -789517, -723724, -657931, -592138, -526345, -460552, -394759, -328966, -263173, -197380, -131587, -65794, -1});

    private LUTReader() {
    }

    static TreeMap<String, LUT> read(String resourcePath) {
        TreeMap<String, LUT> luts = new TreeMap<>(JHVGlobals.alphanumComparator);
        luts.put(gray.name(), gray);

        try (InputStream is = FileUtils.getResource(resourcePath)) {
            parse(is, luts);
        } catch (IOException e) {
            Log.warn("Could not read LUT resource " + resourcePath, e);
        }
        return luts;
    }

    private static void parse(InputStream is, TreeMap<String, LUT> luts) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String name = null;
            int nameLine = 0;
            ArrayList<String> dataLines = new ArrayList<>();
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

    private static void addLut(TreeMap<String, LUT> luts, String name, int nameLine, List<String> dataLines) {
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
            ArrayList<Integer> data = new ArrayList<>(256);
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

    private static void parseDataLine(String line, ArrayList<Integer> data, int lineNo) throws IOException {
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
}
