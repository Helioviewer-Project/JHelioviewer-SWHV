package org.helioviewer.jhv.timelines.band;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.YAxis;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.LinkedListMultimap;

import uk.ac.bristol.star.cdf.AttributeEntry;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.DataType;
import uk.ac.bristol.star.cdf.GlobalAttribute;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;

public class CDFReader {

    public static void load(URI uri) throws Exception {
        List<BandData> lines = read(NetFileCache.get(uri));
        if (lines.isEmpty()) // failed
            return;

        EventQueue.invokeLater(() -> {
            long[] dates = lines.get(0).dates();
            for (BandData line : lines) {
                Band band = Band.createFromType(line.bandType());
                band.addToCache(line.values(), dates);
                Timelines.getLayers().add(band);
            }
            DrawController.setSelectedInterval(dates[0], dates[dates.length - 1]);
        });
    }

    private record BandData(BandType bandType, long[] dates, float[] values) {
    }

    private record DatesValues(long[] dates, float[][] values) {
    }

    private record CDFData(DatesValues datesValues, float scaleMin, float scaleMax, String scaleType, String units,
                           String[] labels) {
    }

    private record CDFVariable(Variable variable, Map<String, String> attributes) {
    }

    private static List<BandData> read(URI uri) throws IOException {
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        LinkedListMultimap<String, String> globalAttrs = LinkedListMultimap.create();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            String name = attr.getName();
            for (AttributeEntry entry : attr.getEntries()) {
                globalAttrs.put(name, entry.toString());
            }
        }
        // dumpGlobalAttrs(globalAttrs);
        String instrumentName = Regex.GT.split(String.join(" ", globalAttrs.get("Descriptor")))[0];

        Variable[] cdfVars = cdf.getVariables();
        VariableAttribute[] cdfAttrs = cdf.getVariableAttributes();

        CDFVariable[] variables = new CDFVariable[cdfVars.length];
        for (int i = 0; i < cdfVars.length; i++) {
            Variable v = cdfVars[i];
            Map<String, String> attrs = new HashMap<>();
            for (VariableAttribute a : cdfAttrs) {
                AttributeEntry e = a.getEntry(v);
                if (e != null)
                    attrs.put(a.getName(), e.toString());
            }
            variables[i] = new CDFVariable(v, attrs);
        }

        long[] dates = readEpoch(variables, uri);
        List<BandData> ret = new ArrayList<>();

        for (CDFVariable v : variables) {
            if ("data".equals(v.attributes.get("VAR_TYPE"))) {
                ret.addAll(readBandData(v, dates, instrumentName, variables, uri));
            }
        }

        return ret;
    }

    private static List<BandData> readBandData(CDFVariable v, long[] dates, String instrumentName, CDFVariable[] variables, URI uri) throws IOException {
        CDFData data = readData(v, dates, instrumentName, variables, uri);
        int numAxes = data.datesValues.values.length;

        List<BandData> ret = new ArrayList<>(numAxes);
        for (int i = 0; i < numAxes; i++) {
            String name = instrumentName + ' ' + data.labels[i];
            JSONObject jo = new JSONObject().
                    put("baseUrl", "").
                    put("unitLabel", data.units).
                    put("name", name).
                    put("range", new JSONArray().put(data.scaleMin).put(data.scaleMax)).
                    put("scale", data.scaleType). //! TBD
                            put("label", name).
                    put("group", "GROUP_CDF");
            //put("bandCacheType", "BandCacheAll");
            ret.add(new BandData(new BandType(jo), data.datesValues.dates, data.datesValues.values[i]));
        }
        return ret;
    }

    private static long[] readEpoch(CDFVariable[] variables, URI uri) throws IOException {
        CDFVariable epoch = null;
        for (CDFVariable v : variables) {
            if ("EPOCH".equalsIgnoreCase(v.variable.getName())) { // mandated by MetadataStandard
                epoch = v;
                break;
            }
        }
        if (epoch == null)
            throw new IOException("Epoch not found: " + uri);

        List<String> timeFillVal = List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000", epoch.attributes.get("FILLVAL"));
        String[][] epochVals = readCDFVariableString(epoch.variable);
        // dumpVariableAttrs(epoch);
        // dumpValues(epochVals);

        // Refuse to fill timestamps
        long[] dates = new long[epochVals.length];
        for (int i = 0; i < dates.length; i++) {
            String epochStr = epochVals[i][0];
            if (timeFillVal.contains(epochStr)) {
                throw new IOException("Filled timestamp (" + epochStr + "): " + uri);
            }
            dates[i] = TimeUtils.parse(epochStr);
        }
        return dates;
    }

    private static CDFData readData(CDFVariable data, long[] dates, String instrumentName, CDFVariable[] variables, URI uri) throws IOException {
        String dataVariableName = data.variable.getName();

        Map<String, String> dataAttrs = data.attributes;
        if (!"EPOCH".equalsIgnoreCase(dataAttrs.get("DEPEND_0")) /*|| !"time_series".equals(dataAttrs.get("DISPLAY_TYPE"))*/) {
            throw new IOException("Inconsistent variable " + dataVariableName + ": " + uri);
        }
        String dataFillVal = dataAttrs.get("FILLVAL");
        String dataScaleTyp = dataAttrs.get("SCALETYP");
        String dataScaleMax = dataAttrs.computeIfAbsent("SCALEMAX", k -> dataAttrs.get("VALIDMAX"));
        String dataScaleMin = dataAttrs.computeIfAbsent("SCALEMIN", k -> dataAttrs.get("VALIDMIN"));
        String dataUnits = dataAttrs.get("UNITS");
        if (dataFillVal == null || dataScaleMax == null || dataScaleMin == null || dataScaleTyp == null || dataUnits == null) {
            throw new IOException("Missing attributes for variable " + dataVariableName + ": " + uri);
        }

        float fillVal = Float.parseFloat(dataFillVal);
        float[][] values = readCDFVariableFloat(data.variable, fillVal);
        // dumpVariableAttrs(data);
        // dumpValues(dataVals);

        int numAxes = values.length;
        int numPoints = values[0].length;
        if (dates.length != numPoints) {
            throw new IOException("Inconsistent lengths of epoch (" + dates.length + ") and data (" + numPoints + ") variables: " + uri);
        }

        String[] labels = new String[numAxes];

        CDFVariable label = null;
        String labelRef = data.attributes.get("LABL_PTR_1");
        for (CDFVariable v : variables) {
            if (v.variable.getName().equals(labelRef)) {
                label = v;
                break;
            }
        }
        if (label == null) {
            String labelAxis = dataAttrs.get("LABLAXIS");
            if (numAxes == 1)
                labels[0] = labelAxis;
            else {
                for (int i = 0; i < numAxes; i++)
                    labels[i] = labelAxis + String.format(" ch_%d", i);
            }
        } else {
            String[][] labelVals = readCDFVariableString(label.variable);
            // dumpVariableAttrs(label);
            // dumpValues(labelVals);
            if (labelVals[0].length != numAxes) {
                throw new IOException("Inconsistent number of labels (" + labelVals[0].length + ") with number of data axes (" + numAxes + "): " + uri);
            }
            for (int i = 0; i < numAxes; i++)
                labels[i] = dataVariableName + ' ' + labelVals[0][i];
        }

        // Temporary
        String datasetId = instrumentName + '_' + data.variable.getName();
        float scaleMin = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> -20;
            default -> Float.parseFloat(dataScaleMin);
        };
        float scaleMax = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> +20;
            default -> Float.parseFloat(dataScaleMax);
        };

        DatesValues rebinned = rebin(new DatesValues(dates, values));
        return new CDFData(rebinned, scaleMin, scaleMax, dataScaleTyp, dataUnits, labels);
    }

    private static class Bin {

        private int n = 0;
        private float mean = 0;

        void add(float val) {
            if (val != YAxis.BLANK) {
                n++;
                mean += (val - mean) / n;
            }
        }

        float getMean() {
            return n == 0 ? YAxis.BLANK : mean;
        }

    }

    private static DatesValues rebin(DatesValues datesValues) {
        long[] dates = datesValues.dates;
        float[][] values = datesValues.values;
        int numAxes = values.length;
        int numPoints = values[0].length;

        long rebinFactor = TimeUtils.MINUTE_IN_MILLIS;
        long startBin = dates[0] / rebinFactor;
        long endBin = dates[dates.length - 1] / rebinFactor;
        int numBins = (int) (endBin - startBin + 1);

        Bin[][] bins = new Bin[numAxes][numBins];
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                bins[j][i] = new Bin();
            }
        }
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numPoints; i++) {
                bins[j][(int) (dates[i] / rebinFactor - startBin)].add(values[j][i]);
            }
        }

        long[] datesBinned = new long[numBins];
        for (int i = 0; i < numBins; i++) {
            datesBinned[i] = (startBin + i) * rebinFactor;
        }
        float[][] valuesBinned = new float[numAxes][numBins];
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                valuesBinned[j][i] = bins[j][i].getMean();
            }
        }

        return new DatesValues(datesBinned, valuesBinned);
    }

    private static String[][] readCDFVariableString(Variable v) throws IOException {
        DataType dataType = v.getDataType();
        int groupSize = dataType.getGroupSize();
        Object abuf = v.createRawValueArray();
        int count = v.getRecordCount();

        String[][] ret = new String[count][];
        for (int j = 0; j < count; j++) {
            v.readRawRecord(j, abuf);
            int len = Array.getLength(abuf);

            String[] out = new String[len / groupSize];
            for (int i = 0; i < len; i += groupSize) {
                out[i / groupSize] = dataType.formatArrayValue(abuf, i);
            }
            ret[j] = out;
        }
        return ret;
    }

    private static float fill(Object o, float fillVal) {
        float val = (float) o;
        return !Float.isFinite(val) || val == fillVal ? YAxis.BLANK : val;
    }

    private static float[][] readCDFVariableFloat(Variable v, float fillVal) throws IOException {
        DataType dataType = v.getDataType();
        Object abuf = v.createRawValueArray();
        int count = v.getRecordCount();

        v.readRawRecord(0, abuf); // read first record to get number of elements
        int len = Array.getLength(abuf);

        float[][] ret = new float[len][count];

        for (int i = 0; i < len; i++)
            ret[i][0] = fill(dataType.getScalar(abuf, i), fillVal); // dubious

        for (int j = 1; j < count; j++) {
            v.readRawRecord(j, abuf);
            int nlen = Array.getLength(abuf);
            if (nlen != len)
                throw new IOException("Inconsistent number of elements: expected " + len + ", got " + nlen);

            for (int i = 0; i < len; i++)
                ret[i][j] = fill(dataType.getScalar(abuf, i), fillVal); // dubious
        }

        return ret;
    }
/*
    private static void dumpGlobalAttrs(LinkedListMultimap<String, String> map) {
        for (String key : map.keySet()) {
            System.out.println(">>> " + key);
            System.out.println("\t" + String.join("\n\t", map.get(key)));
        }
    }

    private static void dumpVariableAttrs(CDFVariable v) {
        System.out.println(">>> " + v.variable.getName());
        for (Map.Entry<String, String> entry : v.attributes.entrySet()) {
            System.out.println("\t" + entry.getKey() + ' ' + entry.getValue());
        }
    }

    private static void dumpValues(String[][] vals) {
        for (String[] val : vals) {
            System.out.println("\t\t" + String.join(" ", val));
        }
    }
*/
}
