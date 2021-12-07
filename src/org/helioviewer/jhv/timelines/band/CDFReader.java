package org.helioviewer.jhv.timelines.band;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.log.Log;
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
        JSONObject jo = read(NetFileCache.get(uri));
        JSONArray ja = jo.optJSONArray("org.helioviewer.jhv.request.timeline");
        if (ja != null) {
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                loadBandResponse(ja.getJSONObject(i));
            }
        }
    }

    private static void loadBandResponse(JSONObject jo) {
        BandResponse response = new BandResponse(jo); // outside EDT

        EventQueue.invokeLater(() -> {
            Band band = Band.createFromType(response.bandType);
            band.addToCache(response.values, response.dates);
            Timelines.getLayers().add(band);
            DrawController.setSelectedInterval(response.dates[0], response.dates[response.dates.length - 1]);
        });
    }

    private record CDFData(BandType bandType, long[] dates, float[] values) {
    }

    private record CDFVariable(Variable variable, Map<String, String> attributes) {
    }

    private static JSONObject read(URI uri) throws IOException {
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        LinkedListMultimap<String, String> globalAttrs = LinkedListMultimap.create();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            String name = attr.getName();
            for (AttributeEntry entry : attr.getEntries()) {
                globalAttrs.put(name, entry.toString());
            }
        }
        String instrumentName = String.join(" ", globalAttrs.get("Instrument_name"));
        String dataProduct = Regex.Space.split(String.join(" ", globalAttrs.get("Data_product")))[0];

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

        JSONObject ret = new JSONObject();

        CDFVariable epoch = null;
        for (CDFVariable v : variables) {
            if ("EPOCH".equals(v.variable().getName())) {
                epoch = v;
                break;
            }
        }
        if (epoch == null) {
            Log.error("Epoch not found: " + uri);
            return ret;
        }

        CDFVariable data = null;
        for (CDFVariable v : variables) {
            Map<String, String> attrs = v.attributes();
            if ("data".equals(attrs.get("VAR_TYPE"))) {
                data = v;
                break;
            }
        }
        if (data == null) {
            Log.error("Data not found: " + uri);
            return ret;
        }

        CDFVariable label = null;
        String labelRef = data.attributes().get("LABL_PTR_1");
        for (CDFVariable v : variables) {
            if (v.variable().getName().equals(labelRef)) {
                label = v;
                break;
            }
        }
        if (label == null) {
            Log.error("Label not found: " + uri);
            return ret;
        }

        Map<String, String> dataAttrs = data.attributes();
        if (!"EPOCH".equals(dataAttrs.get("DEPEND_0")) || !"time_series".equals(dataAttrs.get("DISPLAY_TYPE"))) {
            Log.error("Inconsistent variable " + data.variable.getName() + ": " + uri);
            return ret;
        }
        String dataFillVal = dataAttrs.get("FILLVAL");
        String dataScaleTyp = dataAttrs.get("SCALETYP");
        String dataScaleMax = dataAttrs.get("SCALEMAX");
        String dataScaleMin = dataAttrs.get("SCALEMIN");
        String dataUnits = dataAttrs.get("UNITS");
        if (dataFillVal == null || dataScaleMax == null || dataScaleMin == null || dataScaleTyp == null || dataUnits == null) {
            Log.error("Missing attributes for variable " + data.variable.getName() + ": " + uri);
            return ret;
        }

        List<String> timeFillVal = List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000", epoch.attributes().get("FILLVAL"));
        float fillVal = Float.parseFloat(dataFillVal);

        String[][] epochVals = readVariable(epoch.variable());
        float[][] dataVals = readVariableFloat(data.variable(), fillVal);
        String[][] labelVals = readVariable(label.variable());

        if (epochVals.length != dataVals[0].length) {
            Log.error("Inconsistent lengths of epoch (" + epochVals.length + ") and data (" + dataVals[0].length + ") variables: " + uri);
            return ret;
        }
        if (labelVals[0].length != dataVals.length) {
            Log.error("Inconsistent number of labels (" + labelVals[0].length + ") with number of data axes (" + dataVals.length + "): " + uri);
            return ret;
        }

        // Temporary
        String datasetId = instrumentName + '_' + data.variable().getName();
        float scaleMin = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> -20;
            default -> Float.parseFloat(dataScaleMin);
        };
        float scaleMax = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> +20;
            default -> Float.parseFloat(dataScaleMax);
        };

        // Refuse to fill timestamps
        long[] dates = new long[epochVals.length];
        for (int i = 0; i < epochVals.length; i++) {
            String epochStr = epochVals[i][0];
            if (timeFillVal.contains(epochStr)) {
                Log.error("Filled timestamp (" + epochStr + "): " + uri);
                return ret;
            }
            dates[i] = TimeUtils.parse(epochStr);
        }

        JSONArray ja = new JSONArray();
        for (int j = 0; j < dataVals.length; j++) {
            String name = instrumentName + ' ' + dataProduct + ' ' + labelVals[0][j];
            JSONObject bandType = new JSONObject().
                    put("baseUrl", "").
                    put("unitLabel", dataUnits).
                    put("name", name).
                    put("range", new JSONArray().put(scaleMin).put(scaleMax)).
                    put("scale", dataScaleTyp). //! TBD
                            put("label", name).
                    put("group", "GROUP_CDF");
            //put("bandCacheType", "BandCacheAll");

            JSONArray dataArray = new JSONArray();
            for (int i = 0; i < dataVals[0].length; i++) {
                String epochStr = epochVals[i][0];
                if (!timeFillVal.contains(epochStr)) {
                    long milli = TimeUtils.parse(epochStr) / 1000L; // TBD
                    float val = dataVals[j][i];

                    dataArray.put(new JSONArray().put(milli).put(val));
                }
            }

            ja.put(new JSONObject().put("bandType", bandType).put("data", dataArray));
        }

        // dumpGlobalAttrs(globalAttrs);
        // dumpVariableAttrs(epoch);
        // dumpValues(epochVals);
        // dumpVariableAttrs(data);
        // dumpValues(dataVals);
        // dumpVariableAttrs(label);
        // dumpValues(labelVals);

        return ret.put("org.helioviewer.jhv.request.timeline", ja);
    }

    private static String[][] readVariable(Variable v) throws IOException {
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

    private static float[][] readVariableFloat(Variable v, float fillVal) throws IOException {
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
                throw new IOException("Inconsistent element number: expected " + len + ", got " + nlen);

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
        System.out.println(">>> " + v.variable().getName());
        for (Map.Entry<String, String> entry : v.attributes().entrySet()) {
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