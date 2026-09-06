package org.helioviewer.jhv.plugins.swek.sources;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.event.JHVEventMetadata;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.math.MathUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class HEKParser {

    private static final ThreadLocal<DecimalFormat> formatter1 = ThreadLocal.withInitial(() -> MathUtils.numberFormatter("0", 1));

    static JHVEventMetadata parseResult(JSONObject result, SWEKSupplier supplier, boolean full) throws JSONException {
        JHVEventMetadata.Builder metadata = new JHVEventMetadata.Builder(supplier, full);
        boolean waveCM = false;
        String waveValue = null;

        // First iterate over parameters in the config file
        List<SWEK.Parameter> plist = supplier.getParameterList();
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
                parseRefs(metadata, result.getJSONArray(key));
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
                            metadata.add(lowKey, value);
                    }
                }
            }
        }

        if (waveValue != null) {
            try {
                if (waveCM)
                    waveValue = formatter1.get().format(Double.parseDouble(waveValue) * (1e-2 /*m*/ * 1e9 /*nm*/)) + "nm";
            } catch (Exception ignore) {}
            metadata.add("obs_meanwavel", waveValue);
        }
        return metadata.build();
    }

    private static void parseRefs(JHVEventMetadata.Builder metadata, JSONArray refs) throws JSONException {
        int len = refs.length();
        for (int i = 0; i < len; i++) {
            parseRef(metadata, refs.getJSONObject(i));
        }
    }

    private static void parseRef(JHVEventMetadata.Builder metadata, JSONObject ref) throws JSONException {
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
            metadata.add(type, type, url, true);
        }
    }

}
