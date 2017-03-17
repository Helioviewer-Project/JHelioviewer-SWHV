package org.helioviewer.jhv.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;

@SuppressWarnings("serial")
public class DataSources {

    static final Set<String> SupportedObservatories = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "SOHO", "SDO", "STEREO_A", "STEREO_B", "PROBA2", "ROB-USET", "ROB-Humain", "NSO-GONG", "NSO-SOLIS", "Kanzelhoehe", "NRH", "Yohkoh", "Hinode", "TRACE"
    )));

    private static final HashMap<String, HashMap<String, String>> serverSettings = new HashMap<String, HashMap<String, String>>() {
        {
            put("ROB", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://swhv.oma.be/hv/api/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://swhv.oma.be/hv/api/index.php?action=getJPX&");
                    put("label", "Royal Observatory of Belgium");
                    put("schema", "/data/sources_v1.0.json");
                    put("availability.images", "http://swhv.oma.be/availability/images/availability/availability.html");
                }
            });
            put("IAS", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJPX&");
                    put("label", "Institut d'Astrophysique Spatiale");
                    put("schema", "/data/sources_v1.0.json");
                }
            });
            put("GSFC", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "https://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "https://api.helioviewer.org/v2/getJP2Image/?");
                    put("API.getJPX", "https://api.helioviewer.org/v2/getJPX/?");
                    put("label", "Goddard Space Flight Center");
                    put("schema", "/data/sources_v1.0.json");
                }
            });
            /*
            put("GSFC SCI Test", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJP2Image&");
                    put("API.getJPX", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJPX&");
                    put("label", "Goddard Space Flight Center SCI Test");
                    put("schema", "/data/sources_v1.0.json");
                }
            });
            put("GSFC NDC Test", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJP2Image&");
                    put("API.getJPX", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJPX&");
                    put("label", "Goddard Space Flight Center NDC Test");
                    put("schema", "/data/sources_v1.0.json");
                }
            });
            put("LOCALHOST", new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://localhost:8080/helioviewer/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://localhost:8080/helioviewer/api/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://localhost:8080/helioviewer/api/index.php?action=getJPX&");
                    put("schema", "/data/sources_v1.0.json");
                    put("label", "Localhost");
                }
            });
             */
        }
    };

    public static String[] getServers() {
        Set<String> set = serverSettings.keySet();
        return set.toArray(new String[set.size()]);
    }

    public static String getServerSetting(String server, String setting) {
        Map<String, String> settings = serverSettings.get(server);
        return settings == null ? null : settings.get(setting);
    }

    public static void loadSources() {
        String server = Settings.getSingletonInstance().getProperty("default.server");
        if (server == null || getServerSetting(server, "API.getDataSources") == null)
            server = "ROB";
        Settings.getSingletonInstance().setProperty("default.server", server);

        for (String serverName : serverSettings.keySet())
            JHVGlobals.getExecutorService().execute(new DataSourcesTask(serverName));
    }

}
