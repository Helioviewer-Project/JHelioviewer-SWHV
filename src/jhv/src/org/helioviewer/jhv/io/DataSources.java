package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DataSources {

    private static final HashMap<String, HashMap<String, String>> serverSettings = new HashMap<String, HashMap<String, String>>() {
        {
            put("ROB",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://swhv.oma.be/hv/api/index.php?action=getJP2Image&");
                    put("API.jp2series.path", "http://swhv.oma.be/hv/api/index.php?action=getJPX&");
                    put("default.remote.path", "jpip://swhv.oma.be:8090");
                    put("default.httpRemote.path", "http://swhv.oma.be/hv/jp2/");
                }
            });
            put("IAS",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJP2Image&");
                    put("API.jp2series.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php?action=getJPX&");
                    put("default.remote.path", "jpip://helioviewer.ias.u-psud.fr:8080");
                    put("default.httpRemote.path", "http://helioviewer.ias.u-psud.fr/helioviewer/jp2/");
                }
            });
            put("GSFC",
                new HashMap<String, String>() {
                {
                    put("API.dataSources.path", "http://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=[Yohkoh,STEREO_A,STEREO_B,PROBA2]");
                    put("API.jp2images.path", "http://api.helioviewer.org/v2/getJP2Image/?");
                    put("API.jp2series.path", "http://api.helioviewer.org/v2/getJPX/?");
                    put("default.remote.path", "jpip://helioviewer.org:8090");
                    put("default.httpRemote.path", "http://helioviewer.org/jp2/");
                }
            });
        }
    };

    private static String preferredServer;

    public static String getPreferredServer() {
        return preferredServer;
    }

    public static String getServerSetting(String server, String setting) {
        Map<String, String> settings = serverSettings.get(server);
        if (settings != null)
            return settings.get(setting);
        else
            return null;
    }

    public static void saveServerSettings(String server) {
        Map<String, String> map = serverSettings.get(server);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Settings.getSingletonInstance().setProperty(entry.getKey(), entry.getValue());
        }
    }

    static final HashSet<String> SupportedObservatories = new HashSet<String>();

    public static void loadSources() {
        String prop = Settings.getSingletonInstance().getProperty("supported.data.sources");
        if (prop != null && SupportedObservatories.isEmpty()) {
            String supportedObservatories[] = prop.split(" ");
            for (String s : supportedObservatories) {
                if (!s.isEmpty()) {
                    SupportedObservatories.add(s);
                }
            }
        }

        String datasourcesPath = Settings.getSingletonInstance().getProperty("API.dataSources.path");
        if (datasourcesPath.contains("ias.u-psud.fr")) {
            preferredServer = "IAS";
        } else if (datasourcesPath.contains("helioviewer.org")) {
            preferredServer = "GSFC";
        } else {
            preferredServer = "ROB";
        }
        saveServerSettings(preferredServer);

        try {
            InputStream is = FileUtils.getResourceInputStream("/data/sources_v1.0.json");
            try {
                JSONObject rawSchema = new JSONObject(new JSONTokener(is));
                Schema schema = SchemaLoader.load(rawSchema);

                DataSourcesTask loadTask;
                loadTask = new DataSourcesTask("GSFC", schema);
                JHVGlobals.getExecutorService().execute(loadTask);
                loadTask = new DataSourcesTask("ROB", schema);
                JHVGlobals.getExecutorService().execute(loadTask);
                loadTask = new DataSourcesTask("IAS", schema);
                JHVGlobals.getExecutorService().execute(loadTask);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.error("Could not load the JSON schema: ", e);
        }
    }

}
