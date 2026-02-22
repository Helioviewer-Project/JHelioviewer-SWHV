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
import java.util.Set;

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

public class BandReaderCdf {

    public static void load(URI uri) throws Exception {
        List<Band.Data> lines = read(NetFileCache.get(uri).uri()); // tbd : sniff type
        if (lines.isEmpty()) // failed
            return;
        long[] dates = lines.getFirst().dates();
        if (dates.length == 0) // empty file
            return;

        EventQueue.invokeLater(() -> {
            for (Band.Data line : lines) {
                Band band = Band.createFromType(line.bandType());
                band.addToCache(line.values(), dates);
                Timelines.getLayers().add(band);
            }
            DrawController.setSelectedInterval(dates[0], dates[dates.length - 1]);
        });
    }

    private static final double eV2K = 11604.5250061657;
    private static final Set<String> SWAIncluded = Set.of("N", "V_RTN", "T");

    private record CDFData(DatesValues datesValues, float scaleMin, float scaleMax, String scaleType, String units,
                           String[] labels) {
    }

    private record CDFVariable(Variable variable, Map<String, String> attributes) {
    }

    private static List<Band.Data> read(URI uri) throws IOException {
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        LinkedListMultimap<String, String> globalAttrs = LinkedListMultimap.create();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            String name = attr.getName();
            for (AttributeEntry entry : attr.getEntries()) {
                globalAttrs.put(name, entry.toString());
            }
        }
        // dumpGlobalAttrs(globalAttrs);
        String descriptor = String.join(" ", globalAttrs.get("Descriptor")).trim();
        String[] descriptorParts = Regex.GT.split(descriptor);
        if (descriptorParts.length == 0 || descriptorParts[0].isBlank()) {
            throw new IOException("Missing or invalid Descriptor global attribute: " + uri);
        }
        String instrumentName = descriptorParts[0];

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
        List<Band.Data> ret = new ArrayList<>();

        for (CDFVariable v : variables) {
            if ("data".equals(v.attributes.get("VAR_TYPE"))) {
                if (!"SWA-PAS".equals(instrumentName) || SWAIncluded.contains(v.variable.getName()))
                    ret.addAll(readBandData(v, dates, instrumentName, variables, uri));
            }
        }
        return ret;
    }

    private static List<Band.Data> readBandData(CDFVariable v, long[] dates, String instrumentName, CDFVariable[] variables, URI uri) throws IOException {
        CDFData data = readData(v, dates, instrumentName, variables, uri);
        int numAxes = data.datesValues.values().length;

        List<Band.Data> ret = new ArrayList<>(numAxes);
        for (int i = 0; i < numAxes; i++) {
            String name = instrumentName + ' ' + data.labels[i];
            JSONObject jo = new JSONObject().
                    put("baseUrl", "").
                    put("unitLabel", data.units).
                    put("name", name).
                    put("range", new JSONArray().put(data.scaleMin).put(data.scaleMax)).
                    put("scale", data.scaleType).
                    put("label", "<html>" + name.replaceAll("_(r|t|n|x|y|z|RTN|SRF|VSO|URF)", "<sub>$1</sub>"));
            ret.add(new Band.Data(new BandType(jo), data.datesValues.dates(), data.datesValues.values()[i]));
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

        String fillVal = epoch.attributes.get("FILLVAL");
        List<String> timeFillVal = fillVal == null ?
                List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000") :
                List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000", fillVal); // FILLVAL may be duplicate
        String[][] epochVals = readCDFVariableString(epoch.variable);
        // dumpVariableAttrs(epoch);
        // dumpValues(epochVals);

        // Refuse to fill timestamps
        long[] dates = new long[epochVals.length];
        for (int i = 0; i < dates.length; i++) {
            if (epochVals[i].length == 0) {
                throw new IOException("Empty epoch entry at index " + i + ": " + uri);
            }
            String epochStr = epochVals[i][0];
            if (timeFillVal.contains(epochStr)) {
                throw new IOException("Filled timestamp (" + epochStr + "): " + uri);
            }
            dates[i] = TimeUtils.parse(epochStr);
        }
        return dates;
    }

    private static float parseFloatAttr(String attrName, String value, String variableName, URI uri) throws IOException {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            throw new IOException("Invalid " + attrName + " for variable " + variableName + ": " + value + " (" + uri + ")", e);
        }
    }

    private static CDFData readData(CDFVariable data, long[] dates, String instrumentName, CDFVariable[] variables, URI uri) throws IOException {
        String variableName = data.variable.getName();

        Map<String, String> dataAttrs = data.attributes;
        if (!"EPOCH".equalsIgnoreCase(dataAttrs.get("DEPEND_0")) /*|| !"time_series".equals(dataAttrs.get("DISPLAY_TYPE"))*/) {
            throw new IOException("Inconsistent variable " + variableName + ": " + uri);
        }
        String dataFillVal = dataAttrs.get("FILLVAL");
        String dataScaleTyp = "linear"; //dataAttrs.get("SCALETYP"); -- don't trust value in CDF
        String dataScaleMax = dataAttrs.computeIfAbsent("SCALEMAX", k -> dataAttrs.get("VALIDMAX"));
        String dataScaleMin = dataAttrs.computeIfAbsent("SCALEMIN", k -> dataAttrs.get("VALIDMIN"));
        String dataUnits = dataAttrs.get("UNITS");
        if (dataFillVal == null || dataScaleMax == null || dataScaleMin == null /*|| dataScaleTyp == null*/ || dataUnits == null) {
            throw new IOException("Missing attributes for variable " + variableName + ": " + uri);
        }

        float[][] values = readCDFVariableFloat(data.variable, parseFloatAttr("FILLVAL", dataFillVal, variableName, uri));
        // dumpVariableAttrs(data);
        // dumpValues(dataVals);

        int numAxes = values.length;
        if (numAxes == 0) {
            throw new IOException("No data axes for variable " + variableName + ": " + uri);
        }
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
            if (labelVals.length == 0) {
                throw new IOException("No labels found for variable " + variableName + ": " + uri);
            }
            if (labelVals[0].length != numAxes) {
                throw new IOException("Inconsistent number of labels (" + labelVals[0].length + ") with number of data axes (" + numAxes + "): " + uri);
            }
            for (int i = 0; i < numAxes; i++)
                labels[i] = variableName + ' ' + labelVals[0][i];
        }

        // Temporary
        String datasetId = instrumentName + '_' + variableName;
        float scaleMin = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> -30;
            case "SWA-PAS_V_RTN" -> 200;
            case "SWA-PAS_N" -> 1; // log
            case "SWA-PAS_T" -> 1e3f; // log
            default -> parseFloatAttr("SCALEMIN/VALIDMIN", dataScaleMin, variableName, uri);
        };
        float scaleMax = switch (datasetId) {
            case "MAG_B_RTN", "MAG_B_VSO", "MAG_B_SRF" -> +30;
            case "SWA-PAS_V_RTN" -> 600;
            case "SWA-PAS_N" -> 1e10f; // log
            case "SWA-PAS_T" -> 1e7f; // log
            default -> parseFloatAttr("SCALEMAX/VALIDMAX", dataScaleMax, variableName, uri);
        };

        DatesValues rebinned = new DatesValues(dates, values).rebin();
        if ("SWA-PAS".equals(instrumentName) && "V_RTN".equals(variableName)) { // replace with velocity modulus
            int rNumPoints = rebinned.dates().length;
            float[][] rValues = rebinned.values();
            if (rValues.length < 3)
                throw new IOException("Expected at least 3 axes for " + datasetId + ", got " + rValues.length + ": " + uri);

            float[][] modValues = new float[1][rNumPoints];
            for (int i = 0; i < rNumPoints; i++) {
                float x = rValues[0][i];
                float y = rValues[1][i];
                float z = rValues[2][i];
                if (x == YAxis.BLANK || y == YAxis.BLANK || z == YAxis.BLANK)
                    modValues[0][i] = YAxis.BLANK;
                else
                    modValues[0][i] = (float) Math.sqrt(x * x + y * y + z * z);
            }

            rebinned = new DatesValues(rebinned.dates(), modValues);
            labels = new String[]{"Speed"};
        } else if ("SWA-PAS".equals(instrumentName) && "N".equals(variableName)) { // show log
            dataUnits = "cm^-3";
            dataScaleTyp = "logarithmic";
        } else if ("SWA-PAS".equals(instrumentName) && "T".equals(variableName)) { // transform to Kelvin + show log
            int rNumPoints = rebinned.dates().length;
            float[][] rValues = rebinned.values();
            for (int i = 0; i < rNumPoints; i++) {
                float v = rValues[0][i];
                if (v != YAxis.BLANK)
                    rValues[0][i] = (float) (v * eV2K);
            }
            dataUnits = "K";
            dataScaleTyp = "logarithmic";
        } else if ("MAG".equals(instrumentName) && variableName.startsWith("B_")) { // prepend column with modulus
            int rNumPoints = rebinned.dates().length;
            float[][] rValues = rebinned.values();
            if (rValues.length < 3)
                throw new IOException("Expected at least 3 axes for " + datasetId + ", got " + rValues.length + ": " + uri);

            int rNumAxes = rValues.length;
            float[][] modValues = new float[rNumAxes + 1][];
            modValues[0] = new float[rNumPoints];
            for (int i = 0; i < rNumPoints; i++) {
                float x = rValues[0][i];
                float y = rValues[1][i];
                float z = rValues[2][i];
                if (x == YAxis.BLANK || y == YAxis.BLANK || z == YAxis.BLANK)
                    modValues[0][i] = YAxis.BLANK;
                else
                    modValues[0][i] = (float) Math.sqrt(x * x + y * y + z * z);
            }
            System.arraycopy(rValues, 0, modValues, 1, rNumAxes);
            rebinned = new DatesValues(rebinned.dates(), modValues);

            String[] modLabels = new String[rNumAxes + 1];
            modLabels[0] = variableName + ' ' + "|B|";
            System.arraycopy(labels, 0, modLabels, 1, rNumAxes);
            labels = modLabels;
        }
        return new CDFData(rebinned, scaleMin, scaleMax, dataScaleTyp, dataUnits, labels);
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
        float v = ((Number) o).floatValue();
        return !Float.isFinite(v) || v == fillVal ? YAxis.BLANK : v;
    }

    private static float[][] readCDFVariableFloat(Variable v, float fillVal) throws IOException {
        DataType dataType = v.getDataType();
        Object abuf = v.createRawValueArray();
        int numAxes = Array.getLength(abuf);
        int numPoints = v.getRecordCount();
        String variableName = v.getName();

        float[][] ret = new float[numAxes][numPoints];
        if (numPoints == 0)
            return ret;

        v.readRawRecord(0, abuf);
        for (int i = 0; i < numAxes; i++) {
            try {
                ret[i][0] = fill(dataType.getScalar(abuf, i), fillVal);
            } catch (Exception e) {
                throw new IOException("Invalid numeric value for variable " + variableName, e);
            }
        }

        for (int j = 1; j < numPoints; j++) {
            v.readRawRecord(j, abuf);
            int len = Array.getLength(abuf);
            if (len != numAxes)
                throw new IOException("Inconsistent number of elements: expected " + numAxes + ", got " + len);

            for (int i = 0; i < numAxes; i++) {
                try {
                    ret[i][j] = fill(dataType.getScalar(abuf, i), fillVal);
                } catch (Exception e) {
                    throw new IOException("Invalid numeric value for variable " + variableName, e);
                }
            }
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
