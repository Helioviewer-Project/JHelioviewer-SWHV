package org.helioviewer.jhv.metadata;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public class AIAResponse {

    private record PassBand(String degradationFile, double dataMax, boolean isSqrt) {
    }

    // https://github.com/Helioviewer-Project/jp2gen/blob/master/idl/sdo/aia/hvs_version5_aia.pro
    private static final Map<String, PassBand> passBands = Map.of(
            "131", new PassBand("131_aia_response.json", 500, false),
            "1600", new PassBand("1600_aia_response.json", 400, false),
            "1700", new PassBand("1700_aia_response.json", 5000, true),
            "171", new PassBand("171_aia_response.json", 14000, true),
            "193", new PassBand("193_aia_response.json", 2500, false),
            "211", new PassBand("211_aia_response.json", 1500, false),
            "304", new PassBand("304_aia_response.json", 250, false),
            "335", new PassBand("335_aia_response.json", 80, false),
            "4500", new PassBand("4500_aia_response.json", 20000, false),
            "94", new PassBand("94_aia_response.json", 30, false)
    );

    private record Response(long t_start, long t_stop, double reff_area, double eff_area_p1) {
    }

    private static final Map<String, ArrayList<Response>> response = new HashMap<>();

    private static boolean loaded;

    public static void load() throws Exception {
        for (Map.Entry<String, PassBand> entry : passBands.entrySet()) {
            try (InputStream is = FileUtils.getResource("/data/" + entry.getValue().degradationFile)) {
                JSONObject data = JSONUtils.get(is);
                JSONObject T_START = data.getJSONObject("T_START");
                JSONObject T_STOP = data.getJSONObject("T_STOP");
                JSONObject EFF_AREA = data.getJSONObject("EFF_AREA");
                JSONObject EFFA_P1 = data.getJSONObject("EFFA_P1");

                int length = T_START.length();
                ArrayList<Response> respList = new ArrayList<>(length);

                String key = String.valueOf(0);
                double zeff_area = EFF_AREA.getDouble(key);
                respList.add(new Response(T_START.getLong(key), T_STOP.getLong(key), 1, EFFA_P1.getDouble(key)));

                for (int i = 1; i < length; i++) {
                    key = String.valueOf(i);
                    respList.add(new Response(
                            T_START.getLong(key),
                            T_STOP.getLong(key),
                            EFF_AREA.getDouble(key) / zeff_area,
                            EFFA_P1.getDouble(key)));
                }
                response.put(entry.getKey(), respList);
            }
        }
        loaded = true;
    }

    static double get(long milli, String pass) {
        if (!loaded)
            return 1;

        for (Response r : response.get(pass)) {
            if (milli >= r.t_start && milli < r.t_stop) {
                double factor = 1 / (r.reff_area * (1 + r.eff_area_p1 * (milli - r.t_start) / TimeUtils.DAY_IN_MILLIS));
                // System.out.println(">>> degradation " + (1 / factor));

                PassBand p = passBands.get(pass);
                return p.isSqrt ?
                        Math.sqrt(factor) :
                        1 + Math.log10(factor) / Math.log10(p.dataMax);
            }
        }
        return 1;
    }

}
