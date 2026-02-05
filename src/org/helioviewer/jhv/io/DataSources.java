package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class DataSources {

    public interface Listener {
        void setupSources(DataSourcesParser parser);
    }

    private static final String enabledDatasetsV2 = "[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2,SOLO,GOES-R,IRIS,GONG,ROB,Kanzelhoehe,RHESSI,GOES,PUNCH]";

    private static ImmutableMap<String, Map<String, String>> serverSettings;

    private static Map<String, String> getSourceMap(String api, String label, String availability) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        if (availability != null)
            builder.put("availability.images", availability);

        return builder.
                put("API.getDataSources", api + "getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
                put("API.getJP2Image", api + "getJP2Image/?").
                put("API.getJPX", api + "getJPX/?").
                put("label", label).
                put("schema", "/data/sources_v1.0.json").
                build();
    }

    private static void loadUserServers(JSONObject json, ImmutableMap.Builder<String, Map<String, String>> builder) {
        JSONArray ja = json.optJSONArray("org.helioviewer.jhv.source.image");
        if (ja != null) {
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                try {
                    JSONObject jo = ja.getJSONObject(i);
                    builder.put(jo.getString("name"), getSourceMap(jo.getString("api"), jo.getString("label"), jo.optString("availability")));
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
        }
    }

    private static int toLoad;

    public static void initSources() {
        ImmutableMap.Builder<String, Map<String, String>> builder = new ImmutableMap.Builder<>();
        Path userSources = Path.of(JHVDirectory.SETTINGS.getPath(), "sources.json");
        if (Files.exists(userSources)) { // user servers
            try (BufferedReader reader = Files.newBufferedReader(userSources)) {
                loadUserServers(JSONUtils.get(reader), builder);
            } catch (Exception e) {
                Log.warn(e);
            }
        }

        serverSettings = builder.
                put("ROB", getSourceMap("https://api.swhv.oma.be/hv_docpage/v2/", "Royal Observatory of Belgium", "https://swhv.oma.be/availability/?")).
                put("IAS", getSourceMap("https://helioviewer-api.ias.u-psud.fr/v2/", "Institut d'Astrophysique Spatiale", null)).
                put("GSFC", getSourceMap("https://api.helioviewer.org/v2/", "Goddard Space Flight Center", null)).
                put("ESAC", getSourceMap("https://soar.esac.esa.int/jpip-api/v2/", "European Space Astronomy Center", null)).
                build();
        toLoad = serverSettings.size();
    }

    static Set<String> getServers() {
        return serverSettings.keySet();
    }

    @Nullable
    public static String getServerSetting(@Nonnull String server, @Nonnull String setting) {
        Map<String, String> settings = serverSettings.get(server);
        return settings == null ? null : settings.get(setting);
    }

    public static void loadSources() {
        serverSettings.keySet().forEach(serverName -> LoadSources.submit(serverName));
    }

    private static final ArrayList<Listener> listeners = new ArrayList<>();

    public static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    static void setupSources(@Nullable DataSourcesParser parser) {
        if (parser != null) // didn't fail
            listeners.forEach(listener -> listener.setupSources(parser));

        toLoad--;
        if (toLoad == 0)
            CommandLine.loadRequest();
    }

    private record DatasetId(String server, int sourceId) {
    }

    private record Source(String observatory, String dataset) {
    }

    private static final Map<DatasetId, Source> sourceMap = new ConcurrentHashMap<>();

    static void insert(int sourceId, @Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        sourceMap.put(new DatasetId(server, sourceId), new Source(observatory, dataset));
    }

    static int select(@Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        for (Map.Entry<DatasetId, Source> entry : sourceMap.entrySet()) {
            DatasetId key = entry.getKey();
            if (server.equals(key.server())) {
                Source value = entry.getValue();
                if (value.observatory().contains(observatory) && value.dataset().contains(dataset))
                    return key.sourceId();
            }
        }
        return -1;
    }

}
