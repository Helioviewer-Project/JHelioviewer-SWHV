package org.helioviewer.jhv.metadata;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.DownloadStream;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class AIAResponse {
/*
    // https://github.com/mjpauly/aia/blob/master/mov_img.py
    private static final HashMap<String, Double> STANDARD_INT = new HashMap<String, Double>() {
        {
            put("131", 6.99685);
            put("171", 4.99803);
            put("193", 2.9995);
            put("211", 4.99801);
            put("304", 4.99941);
            put("335", 6.99734);
            put("94", 4.99803);
        }
    };

    private static final HashMap<String, Double> MAX = new HashMap<String, Double>() {
        {
            put("131", 1200.);
            put("171", 6000.);
            put("193", 6000.);
            put("211", 13000.);
            put("304", 2000.);
            put("335", 1000.);
            put("94",  50.);
        }
    };
*/
    // https://github.com/Helioviewer-Project/jp2gen/blob/master/idl/sdo/aia/hvs_version5_aia.pro
    private static final HashMap<String, Double> HV_MAX = new HashMap<String, Double>() {
        {
            put("131", 500.);
            put("171", 14000.);
            put("193", 2500.);
            put("211", 1500.);
            put("304", 250.);
            put("335", 80.);
            put("94",  30.);
        }
    };

//    private static final HashMap<String, Double> LMSAL_MAX = new HashMap<>();

    private static final String extPath = "https://raw.githubusercontent.com/mjpauly/aia/master/";
    private static final String intPath = "/data/";
    private static final String dataFile = "aia_rescaling_data.json";

    private static boolean loaded;
    private static JSONObject responseData;
    private static JSONObject referenceData;
    private static String firstDate;
    private static String lastDate;

    private static URL getExternalURL() throws MalformedURLException {
        return new URL(extPath + dataFile);
    }

    private static URL getInternalURL() {
        return FileUtils.getResourceUrl(intPath + dataFile);
    }

    public static void load() throws Exception {
        try (InputStream is = new DownloadStream(getInternalURL()).getInput()) {
            JSONObject data = JSONUtils.getJSONStream(is);
            String[] keys = JSONObject.getNames(data);
            Arrays.sort(keys);

            firstDate = keys[0];
            lastDate = keys[keys.length - 1];
            responseData = data;
            referenceData = data.getJSONObject("2010-05-01");

            // for (String key : STANDARD_INT.keySet())
            // LMSAL_MAX.put(key, MAX.get(key) / STANDARD_INT.get(key));

            loaded = true;
        }
    }

    static double get(String date, String pass) {
        if (!loaded)
            return 1;

        try {
            if (lastDate.compareTo(date) < 0)
                date = lastDate;
            else if (firstDate.compareTo(date) > 0)
                date = firstDate;

            String key = pass; //+ "_filtered";
            if (!referenceData.has(key) || !responseData.getJSONObject(date).has(key)) // exception if date missing
                return 1;

            double factor, ratio = referenceData.getDouble(key) / responseData.getJSONObject(date).getDouble(key);
            if ("171".equals(pass) || "1700".equals(pass))
                factor = Math.sqrt(ratio);
            else
                factor = 1 + Math.log10(ratio) / Math.log10(HV_MAX.get(pass));
            return factor;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

}
