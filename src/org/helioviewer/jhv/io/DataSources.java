package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

public class DataSources {

    public interface Listener {
        void setupSources(DataSourcesParser parser);
    }

    public record Server(String label, String catalogURL, String jp2URL, String jpxURL, @Nullable String availabilityURL) {}

    private static final String enabledDatasetsV2 = "[MLSO,TRACE,Hinode,Yohkoh,STEREO_A,STEREO_B,PROBA2,SOLO,GOES-R,IRIS,GONG,ROB,Kanzelhoehe,RHESSI,GOES,PUNCH]";

    private static ImmutableMap<String, Server> imageServers;
    private static ImmutableMap<String, String> hapiServers;

    private static Server createImageServer(String api, String label, @Nullable String availability) {
        return new Server(label,
                api + "getDataSources/?verbose=true&enable=" + enabledDatasetsV2,
                api + "getJP2Image/?",
                api + "getJPX/?",
                availability);
    }

    private static JSONObject readUserSources() {
        Path userSources = Path.of(Directories.SETTINGS.getPath(), "sources.json");
        if (Files.exists(userSources)) {
            try (BufferedReader reader = Files.newBufferedReader(userSources)) {
                return JSONUtils.get(reader);
            } catch (Exception e) {
                Log.warn(e);
            }
        }
        return new JSONObject();
    }

    private static ImmutableMap<String, Server> loadImageServers(JSONObject json) {
        ImmutableMap.Builder<String, Server> builder = new ImmutableMap.Builder<>();
        JSONArray entries = json.optJSONArray("org.helioviewer.jhv.source.image");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                try {
                    JSONObject entry = entries.getJSONObject(i);
                    builder.put(entry.getString("name"), createImageServer(entry.getString("api"), entry.getString("label"), entry.optString("availability", null)));
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
        }
        builder.put("ROB", createImageServer("https://api.swhv.oma.be/hv_docpage/v2/", "Royal Observatory of Belgium", "https://swhv.oma.be/availability/?"))
                .put("IAS", createImageServer("https://helioviewer-api.ias.u-psud.fr/v2/", "Institut d'Astrophysique Spatiale", null))
                .put("GSFC", createImageServer("https://api.helioviewer.org/v2/", "Goddard Space Flight Center", null));
        return builder.buildKeepingLast();
    }

    private static ImmutableMap<String, String> loadHapiServers(JSONObject json) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        JSONArray entries = json.optJSONArray("org.helioviewer.jhv.source.hapi");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                try {
                    JSONObject entry = entries.getJSONObject(i);
                    String name = entry.getString("name");
                    String api = entry.getString("api");
                    if (name.isBlank() || api.isBlank())
                        throw new IllegalArgumentException("HAPI server name and api must not be blank");
                    builder.put(name, api.endsWith("/") ? api : api + '/');
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
        }
        builder.put("ROB", "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/");
        return builder.buildKeepingLast();
    }

    public static void initSources() {
        JSONObject json = readUserSources();
        imageServers = loadImageServers(json);
        hapiServers = loadHapiServers(json);
    }

    public static Map<String, String> getHapiServers() {
        return hapiServers;
    }

    public static Set<String> getServers() {
        return imageServers.keySet();
    }

    @Nullable
    public static Server getServer(@Nullable String name) {
        return imageServers.get(name);
    }

    private static Listener listener;

    public static void setListener(Listener _listener) {
        listener = _listener;
    }

    private static int toLoad;
    private static boolean loadCommandLineRequest;

    public static void loadSources(boolean requestAfterLoad) {
        datasetMap.clear(); // clear stale datasets on reload of DataSources
        toLoad = imageServers.size();
        loadCommandLineRequest = requestAfterLoad;
        imageServers.forEach(LoadSources::submit);
    }

    static void setupSources(@Nullable DataSourcesParser parser) {
        if (parser != null && listener != null) // didn't fail
            listener.setupSources(parser);

        toLoad--;
        if (toLoad == 0 && loadCommandLineRequest) {
            loadCommandLineRequest = false;
            CommandLine.loadRequest();
        }
    }

    private record DatasetId(String server, int sourceId) {}

    private record Source(String observatory, String dataset) {}

    private static final Map<DatasetId, Source> datasetMap = new ConcurrentHashMap<>();

    static void insertDataset(int sourceId, @Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        datasetMap.put(new DatasetId(server, sourceId), new Source(observatory, dataset));
    }

    static int selectDataset(@Nonnull String server, @Nonnull String observatory, @Nonnull String dataset) {
        for (Map.Entry<DatasetId, Source> entry : datasetMap.entrySet()) {
            DatasetId key = entry.getKey();
            if (key.server().equals(server)) {
                Source value = entry.getValue();
                if (value.observatory().contains(observatory) && value.dataset().contains(dataset))
                    return key.sourceId();
            }
        }
        return -1;
    }
}
