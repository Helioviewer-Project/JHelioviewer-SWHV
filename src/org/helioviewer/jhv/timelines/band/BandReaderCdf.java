package org.helioviewer.jhv.timelines.band;

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
import org.helioviewer.jhv.timelines.draw.YAxis;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.ac.bristol.star.cdf.AttributeEntry;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.DataType;
import uk.ac.bristol.star.cdf.GlobalAttribute;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;

public class BandReaderCdf {

    private static final double EV_TO_KELVIN = 11604.5250061657;
    private static final Set<String> SWA_INCLUDED = Set.of("N", "V_RTN", "T");

    private record DecodedVariable(DatesValues datesValues, float scaleMin, float scaleMax, String scaleType,
                                   String units, String[] labels) {}

    private record CDFVariable(Variable variable, Map<String, String> attributes) {}

    static List<BandData> read(URI uri) throws Exception {
        uri = NetFileCache.get(uri).uri(); // tbd : sniff type
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        List<String> descriptors = new ArrayList<>();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            if ("Descriptor".equals(attr.getName()))
                for (AttributeEntry entry : attr.getEntries())
                    descriptors.add(entry.toString());
        }
        String descriptor = String.join(" ", descriptors).trim();
        String[] descriptorParts = Regex.GT.split(descriptor);
        if (descriptorParts.length == 0 || descriptorParts[0].isBlank()) {
            throw new IOException("Missing or invalid Descriptor global attribute: " + uri);
        }
        String instrumentName = descriptorParts[0];

        Variable[] cdfVars = cdf.getVariables();
        VariableAttribute[] cdfAttrs = cdf.getVariableAttributes();

        CDFVariable[] variables = new CDFVariable[cdfVars.length];
        Map<String, CDFVariable> variablesByName = new HashMap<>();
        CDFVariable epoch = null;
        for (int i = 0; i < cdfVars.length; i++) {
            Variable v = cdfVars[i];
            Map<String, String> attrs = new HashMap<>();
            for (VariableAttribute a : cdfAttrs) {
                AttributeEntry e = a.getEntry(v);
                if (e != null)
                    attrs.put(a.getName(), e.toString());
            }
            CDFVariable variable = new CDFVariable(v, attrs);
            variables[i] = variable;
            variablesByName.putIfAbsent(v.getName(), variable);
            if (epoch == null && "EPOCH".equalsIgnoreCase(v.getName()))
                epoch = variable;
        }

        long[] dates = readEpoch(epoch, uri);
        List<BandData> ret = new ArrayList<>();

        for (CDFVariable v : variables) {
            if ("data".equals(v.attributes.get("VAR_TYPE"))) {
                if (!"SWA-PAS".equals(instrumentName) || SWA_INCLUDED.contains(v.variable.getName()))
                    ret.addAll(readBandData(v, dates, instrumentName, variablesByName, uri));
            }
        }
        return ret;
    }

    private static List<BandData> readBandData(CDFVariable v, long[] dates, String instrumentName,
                                               Map<String, CDFVariable> variablesByName, URI uri) throws IOException {
        DecodedVariable data = readData(v, dates, instrumentName, variablesByName, uri);
        int numAxes = data.datesValues.values().length;

        List<BandData> ret = new ArrayList<>(numAxes);
        for (int i = 0; i < numAxes; i++) {
            String name = instrumentName + ' ' + data.labels[i];
            JSONObject jo = new JSONObject().
                    put("baseUrl", "").
                    put("unitLabel", data.units).
                    put("name", name).
                    put("range", new JSONArray().put(data.scaleMin).put(data.scaleMax)).
                    put("scale", data.scaleType).
                    put("label", "<html>" + name.replaceAll("_(r|t|n|x|y|z|RTN|SRF|VSO|URF)", "<sub>$1</sub>"));
            ret.add(new BandData(new BandType(jo), data.datesValues.dates(), data.datesValues.values()[i]));
        }
        return ret;
    }

    private static long[] readEpoch(CDFVariable epoch, URI uri) throws IOException {
        if (epoch == null)
            throw new IOException("Epoch not found: " + uri);

        String fillVal = epoch.attributes.get("FILLVAL");
        List<String> timeFillVal = fillVal == null ?
                List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000") :
                List.of("9999-12-31T23:59:59.999999999", "0000-01-01T00:00:00.000000000", fillVal); // FILLVAL may be duplicate
        String[][] epochVals = readCDFVariableString(epoch.variable);

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

    private static DecodedVariable readData(CDFVariable data, long[] dates, String instrumentName,
                                            Map<String, CDFVariable> variablesByName, URI uri) throws IOException {
        String variableName = data.variable.getName();

        Map<String, String> dataAttrs = data.attributes;
        if (!"EPOCH".equalsIgnoreCase(dataAttrs.get("DEPEND_0")) /*|| !"time_series".equals(dataAttrs.get("DISPLAY_TYPE"))*/) {
            throw new IOException("Inconsistent variable " + variableName + ": " + uri);
        }
        String dataFillVal = dataAttrs.get("FILLVAL");
        String scaleType = "linear"; //dataAttrs.get("SCALETYP"); -- don't trust value in CDF
        String dataScaleMax = getAttribute(dataAttrs, "SCALEMAX", "VALIDMAX");
        String dataScaleMin = getAttribute(dataAttrs, "SCALEMIN", "VALIDMIN");
        String dataUnits = dataAttrs.get("UNITS");
        if (dataFillVal == null || dataScaleMax == null || dataScaleMin == null || dataUnits == null) {
            throw new IOException("Missing attributes for variable " + variableName + ": " + uri);
        }

        float[][] values = readCDFVariableFloat(data.variable, parseFloatAttr("FILLVAL", dataFillVal, variableName, uri));

        int numAxes = values.length;
        if (numAxes == 0) {
            throw new IOException("No data axes for variable " + variableName + ": " + uri);
        }
        int numPoints = values[0].length;
        if (dates.length != numPoints) {
            throw new IOException("Inconsistent lengths of epoch (" + dates.length + ") and data (" + numPoints + ") variables: " + uri);
        }

        String[] labels = readLabels(data, numAxes, variablesByName, uri);

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
            float[][] rValues = rebinned.values();
            float[][] modValues = {vectorMagnitude(rValues, datasetId, uri)};
            rebinned = new DatesValues(rebinned.dates(), modValues);
            labels = new String[]{"Speed"};
        } else if ("SWA-PAS".equals(instrumentName) && "N".equals(variableName)) { // show log
            dataUnits = "cm^-3";
            scaleType = "logarithmic";
        } else if ("SWA-PAS".equals(instrumentName) && "T".equals(variableName)) { // transform to Kelvin + show log
            int rNumPoints = rebinned.dates().length;
            float[][] rValues = rebinned.values();
            for (int i = 0; i < rNumPoints; i++) {
                float v = rValues[0][i];
                if (v != YAxis.BLANK)
                    rValues[0][i] = (float) (v * EV_TO_KELVIN);
            }
            dataUnits = "K";
            scaleType = "logarithmic";
        } else if ("MAG".equals(instrumentName) && variableName.startsWith("B_")) { // prepend column with modulus
            float[][] rValues = rebinned.values();
            int rNumAxes = rValues.length;
            float[][] modValues = new float[rNumAxes + 1][];
            modValues[0] = vectorMagnitude(rValues, datasetId, uri);
            System.arraycopy(rValues, 0, modValues, 1, rNumAxes);
            rebinned = new DatesValues(rebinned.dates(), modValues);

            String[] modLabels = new String[rNumAxes + 1];
            modLabels[0] = variableName + ' ' + "|B|";
            System.arraycopy(labels, 0, modLabels, 1, rNumAxes);
            labels = modLabels;
        }
        return new DecodedVariable(rebinned, scaleMin, scaleMax, scaleType, dataUnits, labels);
    }

    private static String[] readLabels(CDFVariable data, int numAxes, Map<String, CDFVariable> variablesByName,
                                       URI uri) throws IOException {
        String variableName = data.variable.getName();
        String[] labels = new String[numAxes];
        CDFVariable label = variablesByName.get(data.attributes.get("LABL_PTR_1"));
        if (label == null) {
            String labelAxis = data.attributes.get("LABLAXIS");
            if (labelAxis == null || labelAxis.isBlank())
                labelAxis = variableName;

            if (numAxes == 1) {
                labels[0] = labelAxis;
            } else {
                for (int i = 0; i < numAxes; i++)
                    labels[i] = labelAxis + String.format(" ch_%d", i);
            }
            return labels;
        }

        String[][] labelValues = readCDFVariableString(label.variable);
        if (labelValues.length == 0)
            throw new IOException("No labels found for variable " + variableName + ": " + uri);
        if (labelValues[0].length != numAxes)
            throw new IOException("Inconsistent number of labels (" + labelValues[0].length
                    + ") with number of data axes (" + numAxes + "): " + uri);

        for (int i = 0; i < numAxes; i++)
            labels[i] = variableName + ' ' + labelValues[0][i];
        return labels;
    }

    private static String getAttribute(Map<String, String> attributes, String name, String fallbackName) {
        String value = attributes.get(name);
        return value != null ? value : attributes.get(fallbackName);
    }

    private static float[] vectorMagnitude(float[][] values, String datasetId, URI uri) throws IOException {
        if (values.length < 3)
            throw new IOException("Expected at least 3 axes for " + datasetId + ", got " + values.length + ": " + uri);

        int length = values[0].length;
        float[] magnitude = new float[length];
        for (int i = 0; i < length; i++) {
            float x = values[0][i];
            float y = values[1][i];
            float z = values[2][i];
            magnitude[i] = x == YAxis.BLANK || y == YAxis.BLANK || z == YAxis.BLANK
                    ? YAxis.BLANK
                    : (float) Math.sqrt(x * x + y * y + z * z);
        }
        return magnitude;
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
        for (int j = 0; j < numPoints; j++) {
            v.readRawRecord(j, abuf);
            int len = Array.getLength(abuf);
            if (len != numAxes)
                throw new IOException("Inconsistent number of elements: expected " + numAxes + ", got " + len);

            try {
                for (int i = 0; i < numAxes; i++)
                    ret[i][j] = fill(dataType.getScalar(abuf, i), fillVal);
            } catch (Exception e) {
                throw new IOException("Invalid numeric value for variable " + variableName, e);
            }
        }
        return ret;
    }

}
