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

import org.everit.json.schema.Validator;
import com.google.common.collect.ImmutableMap;

public class DataSources {

    public interface Listener {
        void setupSources(DataSourcesParser parser);
    }

    private static final String enabledDatasetsV2 = "[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2,SOLO,GOES-R,IRIS,GONG,ROB,Kanzelhoehe,RHESSI,GOES,PUNCH]";

    private static ImmutableMap<String, Map<String, String>> serverSettings;

    private static void loadUserServers(JSONObject json, ImmutableMap.Builder<String, Map<String, String>> builder) {
        JSONArray ja = json.optJSONArray("org.helioviewer.jhv.source.image");
        if (ja != null) {
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                try {
                    JSONObject jo = ja.getJSONObject(i);
                    String name = jo.getString("name");
                    String label = jo.getString("label");
                    String api = jo.getString("api");

                    Map<String, String> map = new ImmutableMap.Builder<String, String>().
                            put("API.getDataSources", api + "getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
                            put("API.getJP2Image", api + "getJP2Image/?").
                            put("API.getJPX", api + "getJPX/?").
                            put("label", label).
                            put("schema", "/data/sources_v1.0.json").
                            build();
                    builder.put(name, map);
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

        builder.
/*    */
        put("ROB", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://api.swhv.oma.be/hv_docpage/v2/getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
        put("API.getJP2Image", "https://api.swhv.oma.be/hv_docpage/v2/getJP2Image/?").
        put("API.getJPX", "https://api.swhv.oma.be/hv_docpage/v2/getJPX/?").
        put("label", "Royal Observatory of Belgium").
        put("schema", "/data/sources_v1.0.json").
        put("availability.images", "https://swhv.oma.be/availability/?").
        build()).
/*    */
        put("IAS", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://helioviewer-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
        put("API.getJP2Image", "https://helioviewer-api.ias.u-psud.fr/v2/getJP2Image/?").
        put("API.getJPX", "https://helioviewer-api.ias.u-psud.fr/v2/getJPX/?").
        put("label", "Institut d'Astrophysique Spatiale").
        put("schema", "/data/sources_v1.0.json").
        build()).
/*
        put("IAS Test", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://inf-helio-test-api.ias.u-psud.fr/v2/getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
        put("API.getJP2Image", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJP2Image/?").
        put("API.getJPX", "https://inf-helio-test-api.ias.u-psud.fr/v2/getJPX/?").
        put("label", "Institut d'Astrophysique Spatiale").
        put("schema", "/data/sources_v1.0.json").
        build()).
*/
        put("GSFC", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://api.helioviewer.org/v2/getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
        put("API.getJP2Image", "https://api.helioviewer.org/v2/getJP2Image/?").
        put("API.getJPX", "https://api.helioviewer.org/v2/getJPX/?").
        put("label", "Goddard Space Flight Center").
        put("schema", "/data/sources_v1.0.json").
        build()).
/*    */
        put("GSFC Beta", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://api.beta.helioviewer.org/v2/getDataSources/?verbose=true&enable=" + enabledDatasetsV2).
        put("API.getJP2Image", "https://api.beta.helioviewer.org/v2/getJP2Image/?").
        put("API.getJPX", "https://api.beta.helioviewer.org/v2/getJPX/?").
        put("label", "Goddard Space Flight Center Beta Server").
        put("schema", "/data/sources_v1.0.json").
        build()).
/*    */
        put("ESAC", new ImmutableMap.Builder<String, String>().
        put("API.getDataSources", "https://soar.esac.esa.int/jpip-api/v2/getDataSources/?verbose=true&enable=[SOLO]").
        put("API.getJP2Image", "https://soar.esac.esa.int/jpip-api/v2/getJP2Image/?").
        put("API.getJPX", "https://soar.esac.esa.int/jpip-api/v2/getJPX/?").
        put("label", "European Space Astronomy Center").
        put("schema", "/data/sources_v1.0.json").
        build());

        serverSettings = builder.build();
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
        Validator validator = Validator.builder().failEarly().build();
        serverSettings.keySet().forEach(serverName -> LoadSources.submit(serverName, validator));
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
