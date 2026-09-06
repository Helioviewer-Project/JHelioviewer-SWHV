package org.helioviewer.jhv.plugins.swek.sources;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.math.MathUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class HEKParser {

    private static final ThreadLocal<DecimalFormat> formatter1 = ThreadLocal.withInitial(() -> MathUtils.numberFormatter("0", 1));

    static void parseResult(JSONObject result, JHVEvent currentEvent, boolean full) throws JSONException {
        boolean waveCM = false;
        String waveValue = null;

        // First iterate over parameters in the config file
        List<SWEK.Parameter> plist = currentEvent.getSupplier().getParameterList();
        Iterator<SWEK.Parameter> paramIterator = plist.iterator();
        HashSet<String> insertedKeys = new HashSet<>();

        Iterator<String> keys = result.keys();
        while (paramIterator.hasNext() || keys.hasNext()) {
            String key = paramIterator.hasNext() ? paramIterator.next().name() : keys.next();
            String lowKey = key.toLowerCase();
            if (!insertedKeys.add(lowKey))
                continue;

            if (result.isNull(lowKey))
                continue;

            if (lowKey.equals("refs")) {
                parseRefs(currentEvent, result.getJSONArray(key));
            } else {
                String value = result.optString(lowKey);
                if (lowKey.equals("rasterscan") || lowKey.equals("bound_chaincode") || lowKey.startsWith("hgc_") || lowKey.startsWith("hgs_") || lowKey.startsWith("hpc_") || lowKey.startsWith("hrc_")) {
                    // nothing, delete
                } else {
                    value = value.trim();
                    if (!value.isEmpty()) {
                        if (lowKey.equals("obs_wavelunit") && value.equals("cm"))
                            waveCM = true;

                        if (lowKey.equals("obs_meanwavel"))
                            waveValue = value;
                        else
                            currentEvent.addParameter(lowKey, value, full);
                    }
                }
            }
        }

        if (waveValue != null) {
            try {
                if (waveCM)
                    waveValue = formatter1.get().format(Double.parseDouble(waveValue) * (1e-2 /*m*/ * 1e9 /*nm*/)) + "nm";
            } catch (Exception ignore) {}
            currentEvent.addParameter("obs_meanwavel", waveValue, full);
        }
    }

    private static void parseRefs(JHVEvent currentEvent, JSONArray refs) throws JSONException {
        int len = refs.length();
        for (int i = 0; i < len; i++) {
            parseRef(currentEvent, refs.getJSONObject(i));
        }
    }

    private static void parseRef(JHVEvent currentEvent, JSONObject ref) throws JSONException {
        String url = "", type = null;

        Iterator<String> keys = ref.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = ref.getString(key);

            String lowerKey = key.toLowerCase();
            if (lowerKey.equals("ref_type")) {
                String lvalue = value.toLowerCase();
                switch (lvalue) {
                    case "movie" -> type = "Reference Movie";
                    case "image" -> type = "Reference Image";
                    case "html" -> type = "Reference Link";
                    default -> {}
                }
            } else if (lowerKey.equals("ref_url")) {
                url = value;
            }
        }
        if (type != null) {
            currentEvent.addParameter(type, type, url, true, true);
        }
    }

}
