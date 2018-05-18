package org.helioviewer.jhv.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.database.DataSourcesDB;

import org.everit.json.schema.Validator;

@SuppressWarnings("serial")
public class DataSources {

    static final Set<String> SupportedObservatories = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "SOHO", "SDO", "STEREO_A", "STEREO_B", "PROBA2", "ROB-USET", "ROB-Humain", "NSO-GONG", "NSO-SOLIS", "Kanzelhoehe", "NRH", "Yohkoh", "Hinode", "TRACE"
    )));

    private static final Map<String, Map<String, String>> serverSettings = Collections.unmodifiableMap(new LinkedHashMap<String, Map<String, String>>() {
        {
            put("ROB", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://swhv.oma.be/hv/api/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://swhv.oma.be/hv/api/index.php?action=getJPX&");
                    put("label", "Royal Observatory of Belgium");
                    put("schema", "/data/sources_v1.0.json");
                    put("availability.images", "http://swhv.oma.be/availability/images/availability/availability.html");
                }
            }));
/*          put("ROB Test", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://swhv2.oma.be:8083/index.php?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://swhv2.oma.be:8083/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://swhv2.oma.be:8083/index.php?action=getJPX&");
                    put("label", "Royal Observatory of Belgium");
                    put("schema", "/data/sources_v1.0.json");
                    put("availability.images", "http://swhv2.oma.be/availability/images/availability/availability.html");
                }
            })); */
            put("IAS", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "https://helioviewer-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "https://helioviewer-api.ias.u-psud.fr/v2/getJP2Image/?");
                    put("API.getJPX", "https://helioviewer-api.ias.u-psud.fr/v2/getJPX/?");
                    put("label", "Institut d'Astrophysique Spatiale");
                    put("schema", "/data/sources_v1.0.json");
                }
            }));
/*          put("IAS Test", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "https://inf-helio-test-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJP2Image/?");
                    put("API.getJPX", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJPX/?");
                    put("label", "Institut d'Astrophysique Spatiale");
                    put("schema", "/data/sources_v1.0.json");
                }
            })); */
            put("GSFC", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "https://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "https://api.helioviewer.org/v2/getJP2Image/?");
                    put("API.getJPX", "https://api.helioviewer.org/v2/getJPX/?");
                    put("label", "Goddard Space Flight Center");
                    put("schema", "/data/sources_v1.0.json");
                }
            }));
/*          put("GSFC SCI Test", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJP2Image&");
                    put("API.getJPX", "http://helioviewer.sci.gsfc.nasa.gov/api.php?action=getJPX&");
                    put("label", "Goddard Space Flight Center SCI Test");
                    put("schema", "/data/sources_v1.0.json");
                }
            }));
            put("GSFC NDC Test", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getDataSources&verbose=true&enable=[TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJP2Image&");
                    put("API.getJPX", "http://gs671-heliovw7.ndc.nasa.gov/api.php?action=getJPX&");
                    put("label", "Goddard Space Flight Center NDC Test");
                    put("schema", "/data/sources_v1.0.json");
                }
            })); */
/*          put("LOCALHOST", Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("API.getDataSources", "http://localhost:8080/helioviewer/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.getJP2Image", "http://localhost:8080/helioviewer/api/index.php?action=getJP2Image&");
                    put("API.getJPX", "http://localhost:8080/helioviewer/api/index.php?action=getJPX&");
                    put("schema", "/data/sources_v1.0.json");
                    put("label", "Localhost");
                }
            })); */
        }
    });

    public static Set<String> getServers() {
        return serverSettings.keySet();
    }

    @Nullable
    public static String getServerSetting(@Nonnull String server, @Nonnull String setting) {
        Map<String, String> settings = serverSettings.get(server);
        return settings == null ? null : settings.get(setting);
    }

    public static void loadSources() {
        String server = Settings.getSingletonInstance().getProperty("default.server");
        if (server == null || getServerSetting(server, "API.getDataSources") == null)
            server = "GSFC";
        Settings.getSingletonInstance().setProperty("default.server", server);

        DataSourcesDB.init();
        Validator validator = Validator.builder().failEarly().build();
        for (String serverName : serverSettings.keySet())
            JHVGlobals.getExecutorService().execute(new DataSourcesTask(serverName, validator));
    }

    private static final HashSet<DataSourcesListener> listeners = new HashSet<>();

    public static void addListener(DataSourcesListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(DataSourcesListener listener) {
        listeners.remove(listener);
    }

    static void setupSources(DataSourcesParser parser) {
        for (DataSourcesListener listener : listeners)
            listener.setupSources(parser);
    }

}
