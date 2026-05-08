package org.helioviewer.jhv.base.lut;

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

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Regex;
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

    private LUTReader() {}

    static Map<String, LUT> read(String resourcePath) {
        TreeMap<String, LUT> luts = new TreeMap<>(JHVGlobals.alphanumComparator);
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
        String[] parts = Regex.Comma.split(line);
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
